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

;; ## Helpers

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
     (verbose "  Trying to retrieve " u " ...")
     (when-let [xml (slurp u)]
       (verbose "  Got " (count xml) " byte(s) of data.")
       xml))))
