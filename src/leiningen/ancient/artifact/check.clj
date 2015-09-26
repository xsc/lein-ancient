(ns leiningen.ancient.artifact.check
  (:require [leiningen.ancient.verbose :refer :all]
            [ancient-clj.core :as ancient]
            [version-clj.core :as version]))

;; ## Artifact Map

(defn- check-artifact?
  [[_ _ & {:keys [upgrade?] :or {upgrade? true}}]]
  (boolean upgrade?))

(defn read-artifact
  "Combine artifact path and artifact vector to a map of `:path`/`:artifact`."
  [path artifact-vector]
  (when (and (vector? artifact-vector)
             (symbol? (first artifact-vector))
             (or (= (count artifact-vector) 1)
                 (string? (second artifact-vector)))
             (check-artifact? artifact-vector))
    {:path path
     :artifact (ancient/read-artifact artifact-vector)}))

(defn- collect-from?
  [{:keys [allowed-keys]} k data]
  (when (contains? allowed-keys k)
    (if (= k :profiles)
      (or (map? data) (sequential? data))
      (sequential? data))))

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

(defn- collect-artifacts-from-map
  [{:keys [allowed-keys] :as options} path artifacts]
  (for [k allowed-keys
        :when (contains? artifacts k)
        :let [data (get artifacts k)]
        :when (collect-from? options k data)
        :let [f (if (= k :profiles)
                  collect-artifacts-from-profiles
                  collect-artifacts-from-vector)]
        artifact (f options (conj path k) data)]
    artifact))

(defn- include-artifact?
  [{:keys [check-clojure?]} {:keys [artifact]}]
  (when artifact
    (or check-clojure?
        (not (contains? #{"clojure"} (:id artifact))))))

(defn collect-artifacts
  "Collect all artifacts in the given map, based on:

   - `:allowed-keys`: a seq of keys to be investiaged (e.g. `[:dependencies]`),
   - `:check-clojure?`: whether to check Clojure artifacts.

   "
  [options artifacts]
  (->> (collect-artifacts-from-map
         (update-in options [:allowed-keys] set)
         []
         artifacts)
       (filter #(include-artifact? options %))))

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
