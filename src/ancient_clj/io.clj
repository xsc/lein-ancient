(ns ^{:doc "IO Helpers"
      :author "Yannick Scherer"}
  ancient-clj.io
  (:require [ancient-clj.verbose :refer :all]
            [clj-http.client :as client :only [get]]))

(defn fetch-url!
  "Fetch the given URL using `slurp` or `clj-http.client/get`."
  [url username password]
  (let [msg (format "  Trying to retrieve '%s' ..." url)
        username (when (seq username) username)]
    (if username
      (verbose msg "(with authentication)")
      (verbose msg))
    (try
      (when-let [response (client/get url {:basic-auth [username password] :as :stream})]
        (with-open [data-stream (:body response)]
          (slurp data-stream)))
      (catch Exception ex
        (let [{:keys [status] :as data} (:object (ex-data ex))]
          (condp = status
            404 (throw (java.io.FileNotFoundException. url))
            401 (throw (ex-info (str "invalid or missing credentials: " url) data))
            (throw ex)))))))

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
  ([url-or-map group-id artifact-id]
   (slurp-metadata! url-or-map nil group-id artifact-id))
  ([url-or-map file-name group-id artifact-id]
   (let [{:keys [url username password]} (if (map? url-or-map)
                                           url-or-map
                                           {:url url-or-map})
         u (build-metadata-url url group-id artifact-id file-name)]
     (when-let [xml (fetch-url! u username password)]
       (verbose "  Got " (count xml) " byte(s) of data.")
       xml))))
