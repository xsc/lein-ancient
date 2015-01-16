(ns leiningen.ancient.artifact.check
  (:require [leiningen.ancient.verbose :refer :all]
            [ancient-clj.core :as ancient]
            [version-clj.core :as version]))

;; ## Artifact Map

(defn read-artifact
  "Combine artifact path and artifact vector to a map of `:path`/`:artifact`."
  [path artifact-vector]
  (when (and (vector? artifact-vector)
             (symbol? (first artifact-vector))
             (string? (second artifact-vector)))
    {:path path
     :artifact (ancient/read-artifact artifact-vector)}))

(defn collect-artifacts
  "Collect all artifacts in the given map, based on `:allowed-keys` within
   the given options."
  ([options artifacts]
   (collect-artifacts options [] artifacts))
  ([{:keys [allowed-keys check-clojure?] :as options} path artifacts]
   (let [p (vec path)
         allowed-key? (set allowed-keys)
         allowed? (fn [k v]
                    (and (allowed-key? k)
                         (sequential? v)))]
     (if (map? artifacts)
       (->> (concat
              (for [[k data] artifacts
                    :when (allowed-key? k)]
                (collect-artifacts
                  options
                  (conj p k)
                  data))
              (for [[k data] artifacts
                    :when (map? data)
                    [k' artifacts'] data
                    :when (allowed? k' artifacts')]
                (collect-artifacts
                  options
                  (conj p k k')
                  artifacts')))
            (reduce concat))
       (cond->> (->> (map-indexed
                       (fn [i artifact-vector]
                         (read-artifact (conj p i) artifact-vector))
                       artifacts)
                     (filter identity))
         (not check-clojure?) (filter (comp not #{"clojure"} :id :artifact)))))))

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
