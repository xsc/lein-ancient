(ns ^{:doc "Maven Metadata Inspection for lein-ancient"
      :author "Yannick Scherer"}
  leiningen.ancient.maven-metadata
  (:require [clojure.data.xml :as xml :only [parse-str]])
  (:use [leiningen.ancient.version :only [version-sort version-map snapshot?]]
        [leiningen.ancient.verbose :only [verbose]]))

;; ## Utilities

(defn- id->path
  "Convert ID to URL path by replacing dots with slashes."
  [^String s]
  (if-not s "" (.replace s "." "/")))

(defn build-metadata-url
  "Get URL to metadata XML file of the given package."
  ([^String repository-url ^String group-id ^String artifact-id]
   (build-metadata-url repository-url group-id artifact-id nil))
  ([^String repository-url ^String group-id ^String artifact-id ^String file-name]
   (str repository-url 
        (if (.endsWith repository-url "/") "" "/")
        (id->path group-id) "/" artifact-id
        "/" 
        (or file-name "maven-metadata.xml"))))

(defn slurp-metadata!
  "Use `slurp` to access metadata."
  ([url group-id artifact-id]
   (slurp-metadata! url nil group-id artifact-id))
  ([url file-name group-id artifact-id]
   (let [u (build-metadata-url url group-id artifact-id file-name)]
     (slurp u))))

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

(defmethod metadata-retriever "http" [m] 
  (partial slurp-metadata! (:url m)))

(defmethod metadata-retriever "https" [m] 
  (partial slurp-metadata! (:url m)))

(defmethod metadata-retriever "file" [m] 
  (let [url (:url m)]
    (fn [group-id artifact-id]
      (or
        (slurp-metadata! url "maven-metadata-local.xml" group-id artifact-id)
        (slurp-metadata! url "maven-metadata.xml" group-id artifact-id)))))

(defmethod metadata-retriever nil [m] nil)

(defn retrieve-metadata!
  "Find metadata XML file in one of the given Maven repositories."
  [retrievers group-id artifact-id]
  (loop [rf retrievers]
    (when (seq rf)
      (if-let [data (try ((first rf) group-id artifact-id) (catch Exception _ nil))]
        data
        (recur (rest rf))))))

;; ## XML Analysis

(defn version-seq
  "Get all the available versions from the given metadata XML string."
  [mta]
  (for [t (try 
            (xml-seq (xml/parse-str mta))
            (catch Exception e
              (verbose "Could not read XML: " (.getMessage e))))
        :when (= (:tag t) :version)]
    (first (:content t))))

(defn- filter-versions
  "Remove all versions that do not fit the given settings map."
  [{:keys [allow-snapshots allow-qualified]} version-maps]
  (let [v version-maps
        v (if-not allow-snapshots (filter (complement snapshot?) v) v)
        v (if-not allow-qualified (filter (comp nil? :qualifier) v) v)]
    v))

(defn latest-version
  "Get map of the latest available version in the given metadata XML
   string."
  ([mta] (latest-version mta nil))
  ([mta settings]
   (->> (version-seq mta)
     (map version-map)
     (filter-versions settings)
     (version-sort)
     (last))))
