(ns ^{ :doc "Repository Handling"
       :author "Yannick Scherer" }
  ancient-clj.repository.core
  (:require [ancient-clj.verbose :refer [verbose]]))

;; ## Registry

(defmulti create-repository
  "Create entity implementing the `Repository` protocol for the given
   repository map (`:url`, `:credentials`, ...)"
  (fn [{:keys [^String url] :as options}]
    (when url
      (let [i (.indexOf url ":/")]
        (when-not (neg? i)
          (.substring url 0 i)))))
  :default nil)

(defmethod create-repository nil [& _] nil)

;; ## Protocol

(defprotocol Repository
  (retrieve-metadata-xml! [this group-id artifact-id]
    "Retrieve the version metadata XML file as a String."))

(extend-protocol Repository
  clojure.lang.AFunction
  (retrieve-metadata-xml! [f group-id artifact-id]
    (f group-id artifact-id))
  nil
  (retrieve-metadata-xml! [this group-id artifact-id]
    nil))
