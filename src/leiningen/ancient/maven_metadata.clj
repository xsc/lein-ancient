(ns ^{:doc "Maven Metadata Inspection for lein-ancient"
      :author "Yannick Scherer"}
  leiningen.ancient.maven-metadata
  (:require [clojure.data.xml :as xml :only [parse-str]]
            [leiningen.ancient.version :refer [version-sort version-map snapshot? qualified?]]
            [leiningen.ancient.verbose :refer [verbose]]))

;; ## Metadata Retrieval

(defmulti metadata-retriever
  "Multimethod that takes a repository map (:url, ...) and produces a function that can access 
   artifact metadata using group-id and artifact-id."
  (fn [repository-map]
    (let [^String url (:url repository-map)
          i (.indexOf url ":/")]
      (when-not (neg? i)
        (.substring url 0 i))))
  :default nil)

(defmethod metadata-retriever nil [m] nil)

(defn metadata-retrievers
  "Get seq of retriever functions from seq of repository maps."
  [repository-maps]
  (->> repository-maps
    (map metadata-retriever)
    (filter (complement nil?))))

(defn retrieve-metadata!
  "Find metadata XML file(s) in the given Maven repositories. Returns a seq of XML strings."
  [retrievers settings group-id artifact-id]
  (loop [rf retrievers
         rx []]
    (if-not (seq rf)
      rx
      (let [[retrieve! & rst] rf] 
        (if-let [data (try (retrieve! group-id artifact-id) (catch Exception _ nil))]
          (if (:aggressive settings)
            (recur rst (conj rx data))
            (vector data))
          (recur rst rx))))))

;; ## XML Analysis

(defn version-seq
  "Get all the available versions from the given metadata XML string(s)."
  [mta]
  (if (string? mta)
    (for [t (try 
              (xml-seq (xml/parse-str mta))
              (catch Exception e
                (verbose "Could not read XML: " (.getMessage e))))
          :when (= (:tag t) :version)]
      (first (:content t)))
    (mapcat version-seq mta)))

(defn filter-versions
  "Remove all versions that do not fit the given settings map."
  [{:keys [allow-snapshots allow-qualified]} version-maps]
  (let [v version-maps
        v (if-not allow-snapshots (filter (complement snapshot?) v) v)
        v (if-not allow-qualified (filter #(or (snapshot? %) (not (qualified? %))) v) v)]
    v))

(defn latest-version
  "Get map of the latest available version in the given metadata XML
   string."
  ([mta] (latest-version mta nil))
  ([mta settings]
   (let [vs (version-seq mta)]
     (->> vs
       (map version-map)
       (filter-versions settings)
       (version-sort)
       (last)))))
