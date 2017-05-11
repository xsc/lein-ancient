(ns ancient-clj.io.s3
  (:require [ancient-clj.io.xml :as xml])
  (:import (com.amazonaws.auth AWSCredentialsProvider BasicAWSCredentials DefaultAWSCredentialsProviderChain)
           (com.amazonaws.services.s3 AmazonS3ClientBuilder)
           (com.amazonaws.services.s3.model AmazonS3Exception)))

(def ^:private valid-content-types
  #{"text/xml" "application/xml"})

(defn ^:private s3-get-object!
  [client bucket key]
  (let [s3-object (.getObject client bucket key)]
    {:content (.getObjectContent s3-object)
     :content-type (.getContentType (.getObjectMetadata s3-object))}))

(defn s3-loader
  "Create version loader for S3 repository."
  [bucket & [{:keys [path username passphrase no-auth]
              :or {path "releases"}}]]
  {:pre [(or no-auth (string? username))
         (or no-auth (string? passphrase))]}
  (let [credentials (if no-auth
                      (DefaultAWSCredentialsProviderChain.)
                      (let [creds (BasicAWSCredentials. username passphrase)]
                        (reify AWSCredentialsProvider
                          (getCredentials [_] creds)
                          (refresh [_]))))
        client (-> (AmazonS3ClientBuilder/standard)
                   (.withCredentials credentials)
                   (.build))
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
