(ns ^{ :doc "HTTP Repository Handling"
       :author "Yannick Scherer" }
  ancient-clj.repository.http
  (:require [ancient-clj.repository.core :refer [create-repository]]
            [ancient-clj.io :refer [slurp-metadata!]]))

(defmethod create-repository "http" [m]
  (partial slurp-metadata! m))

(defmethod create-repository "https" [m]
  (partial slurp-metadata! m))
