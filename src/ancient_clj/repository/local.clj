(ns ^{ :doc "HTTP Repository Handling"
       :author "Yannick Scherer" }
  ancient-clj.repository.http
  (:require [ancient-clj.repository.core :refer [create-repository]]
            [ancient-clj.io :refer [slurp-metadata!]]))

(defmethod create-repository "file" [m]
  (let [url (:url m)]
    (fn [group-id artifact-id]
      (or
        (slurp-metadata! url "maven-metadata-local.xml" group-id artifact-id)
        (slurp-metadata! url "maven-metadata.xml" group-id artifact-id)))))
