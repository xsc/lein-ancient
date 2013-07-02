(ns ^{ :doc "HTTP/HTTPS Repositories"
       :author "Yannick Scherer" }
  leiningen.ancient.maven-metadata.http
  (:use [leiningen.ancient.maven-metadata :only [metadata-retriever]]
        [leiningen.ancient.maven-metadata.utils :only [slurp-metadata!]]))

(defmethod metadata-retriever "http" [m] 
  (partial slurp-metadata! (:url m)))

(defmethod metadata-retriever "https" [m] 
  (partial slurp-metadata! (:url m)))
