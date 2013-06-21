(ns ^{:doc "Maven Metadata Inspection for lein-ancient"
      :author "Yannick Scherer"}
  leiningen.ancient.maven-metadata
  (:require [clojure.data.xml :as xml :only [parse-str]])
  (:use [leiningen.ancient.version :only [version-sort version-map snapshot?]]))

;; ## Utilities

(defn- id->path
  "Convert ID to URL path by replacing dots with slashes."
  [^String s]
  (if-not s "" (.replace s "." "/")))

(defn build-metadata-url
  "Get URL to maven-metadata.xml of the given package."
  [^String repository-url ^String group-id ^String artifact-id]
  (str repository-url 
       (if (.endsWith repository-url "/") "" "/")
       (id->path group-id) "/" artifact-id
       "/maven-metadata.xml"))

;; ## Metadata Retrieval

(defn slurp-metadata!
  "Use `slurp` to access metadata."
  [url group-id artifact-id]
  (let [u (build-metadata-url url group-id artifact-id)]
    (slurp u)))

(defmulti find-retriever
  "Multimethod that takes a repository map (:url, ...) and produces a function that can access 
   artifact metadata using group-id and artifact-id."
  (fn [repository-map]
    (let [^String url (:url repository-map)
          i (.indexOf url ":/")]
      (when-not (neg? i)
        (.substring url 0 i))))
  :default nil)

(defmethod find-retriever "http" [m] (partial slurp-metadata! (:url m)))
(defmethod find-retriever "https" [m] (partial slurp-metadata! (:url m)))
(defmethod find-retriever "file" [m] (partial slurp-metadata! (:url m)))
(defmethod find-retriever nil [m] nil)

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
  (for [t (xml-seq (xml/parse-str mta))
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
