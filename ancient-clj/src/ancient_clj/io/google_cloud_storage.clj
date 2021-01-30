(ns ancient-clj.io.google-cloud-storage
  (:require [ancient-clj.io.xml :as xml])
  (:import [com.google.cloud.storage Blob$BlobSourceOption BlobId StorageException StorageOptions]))

(def ^:private valid-content-types
  #{"text/xml" "application/xml" "application/octet-stream"})

(defn- build-client-delay
  "Creates a client in delay, possibly using credentials given with the
  options."
  [options]
  (delay (.getService (StorageOptions/getDefaultInstance))))

(defn- get-object!
  "Gets an Google Cloud Storage object at the given bucket and key. The
  client-ref is a client which must be dereferenced to be
  used (permitting lazy evaluation)."
  [client-ref bucket key]
  (when-let [blob (.get @client-ref (BlobId/of bucket key))]
    {:content (.getContent blob (into-array Blob$BlobSourceOption []))
     :content-type (.getContentType blob)}))

(defn google-cloud-storage-loader
  "Create version loader for a Google Cloud Storage repository."
  [bucket & [{:keys [path]
              :or {path "releases"}
              :as options}]]
  (let [client (build-client-delay options)
        get! #(get-object! client bucket %)]
    (fn [group id]
      (try
        (let [object-id (xml/metadata-uri path group id)
              {:keys [content content-type]} (get! object-id)
              content-type (and content-type (first (.split content-type ";")))]
          (if content-type
            (if (contains? valid-content-types content-type)
              (if content
                (xml/metadata-xml->versions (slurp content))
                (Exception. "object content not found."))
              (Exception.
               (format "object's content-type is not XML (%s): %s"
                       (pr-str valid-content-types)
                       content-type)))
            []))
        (catch StorageException ex
          (Exception. (format "[code=%d] %s" (.getCode ex) (.getMessage ex)) ex))
        (catch Throwable ex
          ex)))))
