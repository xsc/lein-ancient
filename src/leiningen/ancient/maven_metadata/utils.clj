(ns leiningen.ancient.maven-metadata.utils
  (:use [leiningen.ancient.verbose :only [verbose]]))

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
