(ns leiningen.ancient.artifact.check
  (:require [leiningen.ancient.verbose :refer :all]
            [ancient-clj.core :as ancient]
            [version-clj.core :as version]))

;; ## Artifact Keys/Partitions

(defn- artifact-keys
  [{:keys [id version]} [_ _ & {:keys [upgrade upgrade?] :or {upgrade []}}]]
  (if (or (false? upgrade)
          (false? upgrade?)
          (contains? #{[[] ["snapshot"]]
                       [["release"]]
                       [["latest"]]}
                     version))
    [::never]
    (cond-> #{}
      (#{"clojure"} id)            (conj :clojure)
      (#{"clojurescript"} id)      (conj :clojure)
      (version/qualified? version) (conj :qualified)
      (version/snapshot? version)  (-> (conj :snapshots) (disj :qualified))
      (keyword? upgrade)           (conj upgrade)
      (sequential? upgrade)        (concat upgrade))))

;; ## Artifact Collection

(defn read-artifact
  "Combine artifact path and artifact vector to a map of `:path`/`:artifact`."
  [path artifact-vector]
  (when (and (vector? artifact-vector)
             (symbol? (first artifact-vector))
             (or (= (count artifact-vector) 1)
                 (string? (second artifact-vector))))
    (let [artifact (ancient/read-artifact artifact-vector)
          ks (artifact-keys artifact artifact-vector)]
      {:path     path
       :artifact artifact
       :keys     ks})))

(defn- collect-from?
  [k data]
  (if (= k :profiles)
    (or (map? data) (sequential? data))
    (sequential? data)))

(declare collect-artifacts-from-map)

(defn- collect-artifacts-from-composite-profile
  [options path data]
  (->> (keep-indexed
         (fn [i artifact-map]
           (when (map? artifact-map)
             (collect-artifacts-from-map
               options
               (conj path i)
               artifact-map)))
         data)
       (reduce concat)))

(defn- collect-artifacts-from-profiles
  [options path profiles]
  (mapcat
    (fn [[profile data]]
      (let [path' (conj path profile)]
        (if (map? data)
          (collect-artifacts-from-map options path' data)
          (collect-artifacts-from-composite-profile options path' data))))
    profiles))

(defn- collect-artifacts-from-vector
  [options path v]
  (keep-indexed
    (fn [i artifact-vector]
      (read-artifact
        (conj path i)
        artifact-vector))
    v))

(defn- remove-managed-dependencies
  "Managed dependencies appear twice:
   - once in `:dependencies` but without version,
   - and once in `:managed-dependencies`, this time with version.

   We don't want to add a version to `:dependencies` but we want to
   check/update the one in `:managed-dependencies` - thus, we have to
   remove all entries in `:dependencies` that have a managed counterpart.

   We expect the list of dependencies to treat as managed as an option
   value to handle inheritance better."
  [{:keys [managed-dependencies]} artifacts]
  (let [managed-artifacts (set (map first managed-dependencies))]
    (remove
      (fn [{{:keys [symbol version-string]} :artifact, ks :keys}]
        (and (not (contains? (set ks) :managed-dependencies))
             (empty? version-string)
             (contains? managed-artifacts symbol)))
      artifacts)))

(defn- collect-artifacts-from-map
  [options path artifacts]
  (for [k [:dependencies :managed-dependencies :plugins :profiles :java-agents]
        :when (contains? artifacts k)
        :let [data (get artifacts k)]
        :when (collect-from? k data)
        :let [f (if (= k :profiles)
                  collect-artifacts-from-profiles
                  collect-artifacts-from-vector)
              artifact-key (if (= k :managed-dependencies)
                             :dependencies
                             k)]
        artifact (f options (conj path k) data)]
    (-> artifact
        (update :keys conj k artifact-key))))

(defn- match-it
  [f sq keys]
  (f
    (fn [item]
      (if (sequential? item)
        (every? keys (set item))
        (contains? keys item)))
    sq))

(defn- mark-artifact
  [{:keys [include exclude]} {:keys [artifact keys] :as data}]
  (when artifact
    (let [k? (set keys)]
      (when-not (k? ::never)
        (cond (and (or (empty? include) (match-it some include k?))
                   (or (empty? exclude) (match-it not-any? exclude k?)))
              (assoc data :include? true)

              (k? :clojure)
              data)))))

(defn collect-artifacts
  "Collect all artifacts in the given map, based on:

   - `:include`: artifact-specific keys to include,
   - `:exclude`: artifact-specific keys to exclude.

   Artifact keys are derived from the artifact ID, its position, version and an
   additional `:upgrade` option in the artifact vector. The following would e.g.
   have the keys `:snapshot`, `:clojure`, `:dependencies` and `:profiles`:

       :profiles {:1.7 {:dependencies
                        [[org.clojure/clojure \"1.7.0-master-SNAPSHOT\"]]}}}

   "
  [options artifacts]
  (->> (collect-artifacts-from-map options [] artifacts)
       (remove-managed-dependencies options)
       (keep #(mark-artifact options %))))

;; ## Check

(defn- latest-version-with-cache!
  "Use the current cache to resolve the latest version of the given artifact."
  [{:keys [cache] :as opts}
   {:keys [group id] :as artifact}]
  (let [k [group id]
        d (delay
            (ancient/latest-version!
              artifact
              opts))]
    @(dosync
       (get
         (if (contains? @cache k)
           @cache
           (alter cache assoc k d))
         k))))

(defn check-artifact!
  "Check the given artifact data, associating `:latest` into it
   if the given version is outdated. Otherwise, `nil` will be returned."
  [options {:keys [path artifact] :as data}]
  (debugf "-- artifact %s at %s ... (%d repositories)"
          (pr-str (:form artifact))
          (pr-str path)
          (count (:repositories options)))
  (when-let [latest (latest-version-with-cache!
                      options
                      artifact)]
    (when (neg? (version/version-seq-compare
                  (:version artifact)
                  (:version latest)))
      (debugf "-- artifact %s is outdated. (latest: %s)"
              (pr-str (:form artifact))
              (pr-str (:version-string latest)))
      (assoc data :latest latest))))

(defn check-artifacts!
  "Check the given seq of artifacts using `pmap`."
  [options artifacts]
  (->> (pmap
         #(check-artifact! options %)
         artifacts)
       (filter identity)))

(defn collect-and-check-artifacts!
  "Collect and check all artifacts contained within the given data."
  [options data]
  (->> data
       (collect-artifacts options)
       (check-artifacts! options)))
