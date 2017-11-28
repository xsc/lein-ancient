(ns ancient-clj.io.s3
  (:require [ancient-clj.io.xml :as xml])
  (:import (com.amazonaws.auth AWSCredentialsProvider BasicAWSCredentials)
           (com.amazonaws.services.s3 AmazonS3ClientBuilder)
           (com.amazonaws.services.s3.model AmazonS3Exception)))

(def ^:private valid-content-types
  #{"text/xml" "application/xml"})

(defn- static-credentials-provider
  [{access-key :username, secret-key :passphrase}]
  {:pre [(string? access-key)
         (string? secret-key)]}
  (let [creds (BasicAWSCredentials. access-key secret-key)]
    (reify AWSCredentialsProvider
      (getCredentials [_] creds)
      (refresh [_]))))

(defn- check-credentials!
  [{:keys [username passphrase]}]
  (when-not (or (every? nil? [username passphrase])
                (every? string? [username passphrase]))
    (throw
      (IllegalArgumentException.
        (str  "You have to supply both ':username' and ':passphrase' for S3 "
              "repositories.\n"
              "Note that you can omit both to fall back to your system's AWS "
              "credentials.")))))

(defn- build-client-delay
  "Creates a client in delay, possibly using credentials given with the
  options."
  [options]
  (check-credentials! options)
  (delay
   (cond-> (AmazonS3ClientBuilder/standard)
     (:username options) (.withCredentials
                           (static-credentials-provider options))
     (:region options)   (.withRegion (:region options))
     true (.build))))

(defn- s3-get-object!
  "Gets an S3 object at the given bucket and key.  The client-ref is a client
  which must be dereferenced to be used (permitting lazy evaluation)."
  [client-ref bucket key]
  (let [s3-object (.getObject @client-ref bucket key)]
    {:content      (.getObjectContent s3-object)
     :content-type (.getContentType (.getObjectMetadata s3-object))}))

(defn s3-loader
  "Create version loader for S3 repository."
  [bucket & [{:keys [path]
              :or {path "releases"}
              :as options}]]
  (let [client (build-client-delay options)
        get! #(s3-get-object! client bucket %)]
    (fn [group id]
      (try
        (let [object-id (xml/metadata-uri path group id)
              {:keys [content content-type]} (get! object-id)
              content-type (and content-type (first (.split content-type ";")))]
          (if (contains? valid-content-types content-type)
            (if content
              (xml/metadata-xml->versions (slurp content))
              (Exception. "object content not found."))
            (Exception.
              (format "object's content-type is not XML (%s): %s"
                      (pr-str valid-content-types)
                      content-type))))
        (catch AmazonS3Exception ex
          (let [s (.getStatusCode ex)]
            (if (= s 404)
              []
              (Exception.
                (format "[status=%d] %s" s (.getErrorCode ex))
                ex))))
        (catch Throwable ex
          ex)))))
