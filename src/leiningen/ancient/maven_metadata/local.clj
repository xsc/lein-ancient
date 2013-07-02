(ns ^{ :doc "Local Repositories" 
       :author "Yannick Scherer" }
  leiningen.ancient.maven-metadata.local
  (:use [leiningen.ancient.maven-metadata :only [metadata-retriever]]
        [leiningen.ancient.maven-metadata.utils :only [slurp-metadata!]]))

(defmethod metadata-retriever "file" [m] 
  (let [url (:url m)]
    (fn [group-id artifact-id]
      (or
        (slurp-metadata! url "maven-metadata-local.xml" group-id artifact-id)
        (slurp-metadata! url "maven-metadata.xml" group-id artifact-id)))))
