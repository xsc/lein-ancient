(ns ^{ :doc "Repository Access for ancient-clj."
       :author "Yannick Scherer" }
  ancient-clj.repository
  (:require [ancient-clj.repository.core :as r]
            [ancient-clj.repository http local s3p]
            [ancient-clj.verbose :refer [verbose]]
            [clojure.data.xml :as xml :only [parse-str]]))

;; ## Wrapper

(def repository
  "Create entity implementing the `Repository` protocol for either a
   repository URL or a repository map."
  (memoize
    (fn [r]
      (if (string? r)
        (r/create-repository {:url r})
        (r/create-repository r)))))

(extend-protocol r/Repository
  java.lang.String
  (retrieve-metadata-xml! [this group-id artifact-id]
    (when-let [repo (repository {:url this})]
      (r/retrieve-metadata-xml! repo group-id artifact-id)))
  clojure.lang.IPersistentMap
  (retrieve-metadata-xml! [this group-id artifact-id]
    (when-let [repo (repository this)]
      (r/retrieve-metadata-xml! repo group-id artifact-id))))

;; ## Standard Repositories

(def ^:dynamic *repositories*
  "Repositories to use for metadata retrieval if none are given."
  (vector
    (repository "http://repo1.maven.org/repo")
    (repository "https://clojars.org/repo")))

;; ## Access Functions

(defn retrieve-metadata-xml! 
  "Retrieve the version metadata XML file as a String searching the given 
   repositories. The first found result will be returned."
  ([group-id artifact-id] 
   (retrieve-metadata-xml! *repositories* group-id artifact-id))
  ([repos group-id artifact-id]
   (loop [repos repos]
     (when (seq repos)
       (or 
         (r/retrieve-metadata-xml! (first repos) group-id artifact-id)
         (recur (rest repos)))))))

(defn retrieve-versions!
  "Retrieve a seq of version strings for the given artifact from the given
   repositories. The first found result will be returned."
  ([group-id artifact-id]
   (retrieve-versions! *repositories* group-id artifact-id))
  ([repos group-id artifact-id]
   (loop [repos repos]
     (when (seq repos)
       (let [[repo & rst] repos]
         (or
           (when-let [mta (r/retrieve-metadata-xml! repo group-id artifact-id)]
             (when (string? mta)
               (for [t (try 
                         (xml-seq (xml/parse-str mta))
                         (catch Exception e
                           (verbose "Could not read XML: " (.getMessage e))))
                     :when (= (:tag t) :version)]
                 (first (:content t)))))
           (recur rst)))))))

(defn retrieve-all-versions!
  "Retrieve a seq of all available versions for the given artifact from all of the given
   repositories."
  ([group-id artifact-id] 
   (retrieve-all-versions! *repositories* group-id artifact-id))
  ([repos group-id artifact-id]
   (mapcat
     (fn [repo]
       (retrieve-versions! [repo] group-id artifact-id))
     repos)))
