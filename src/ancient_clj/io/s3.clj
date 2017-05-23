(ns ancient-clj.io.s3
  (:require [ancient-clj.io.xml :as xml])
  (:import (com.amazonaws.auth AWSCredentialsProvider BasicAWSCredentials DefaultAWSCredentialsProviderChain)
           (com.amazonaws.services.s3 AmazonS3ClientBuilder)
           (com.amazonaws.services.s3.model AmazonS3Exception)))

(def ^:private valid-content-types
  #{"text/xml" "application/xml"})

(defn ^:private create-static-credentials-provider
  "Creates a static credentials credentials provider using the supplied
  credentials."
  [access-key secret-key]
  {:pre [(string? access-key)
         (string? secret-key)]}
  (let [creds (BasicAWSCredentials. access-key secret-key)]
    (reify AWSCredentialsProvider
      (getCredentials [_] creds)
      (refresh [_]))))

(defn ^:private create-credentials-provider
  "Creates a credentials provider using the given username and password, if
  any.  If no credentials have been provided, use the default credentials
  provider."
  [{:keys [username passphrase]}]
  (if (or (some? username) (some? passphrase))
    (create-static-credentials-provider username passphrase)
    (DefaultAWSCredentialsProviderChain.)))

(defn ^:private create-delayed-client
  "Creates a client in delay, possibly using credentials given with the
  options."
  [{:keys [username passphrase] :as options}]
  (when (or (some? username) (some? passphrase))
    (assert (and (string? username) (string? passphrase))))
  (delay
    (-> (AmazonS3ClientBuilder/standard)
        (.withCredentials (create-credentials-provider options))
        (.build))))

(defn ^:private s3-get-object!
  "Gets an S3 object at the given bucket and key.  The client-ref is a client
  which must be dereferenced to be used (permitting lazy evaluation)."
  [client-ref bucket key]
  (let [s3-object (.getObject @client-ref bucket key)]
    {:content (.getObjectContent s3-object)
     :content-type (.getContentType (.getObjectMetadata s3-object))}))

(defn s3-loader
  "Create version loader for S3 repository."
  [bucket & [{:keys [path]
              :or {path "releases"}
              :as options}]]
  (let [client (create-delayed-client options)
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
