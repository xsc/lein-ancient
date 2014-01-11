(ns ^{ :doc "HTTP Repository Handling"
       :author "Yannick Scherer" }
  ancient-clj.repository.http
  (:require [ancient-clj.repository.core :refer [create-repository slurp-metadata!]]))

(defmethod create-repository "http" [m]
  (partial slurp-metadata! (:url m)))

(defmethod create-repository "https" [m]
  (partial slurp-metadata! (:url m)))
