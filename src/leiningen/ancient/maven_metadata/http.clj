(ns ^{ :doc "HTTP/HTTPS Repositories"
       :author "Yannick Scherer" }
  leiningen.ancient.maven-metadata.http
  (:require [leiningen.ancient.maven-metadata :refer [metadata-retriever]]
            [leiningen.ancient.maven-metadata.utils :refer [slurp-metadata!]]))

(defmethod metadata-retriever "http" [m] 
  (partial slurp-metadata! (:url m)))

(defmethod metadata-retriever "https" [m] 
  (partial slurp-metadata! (:url m)))
