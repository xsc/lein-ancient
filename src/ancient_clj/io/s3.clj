(ns ancient-clj.io.s3
  (:require [ancient-clj.io.xml :as xml]
            [aws.sdk.s3 :as s3])
  (:import [com.amazonaws.services.s3.model AmazonS3Exception]))

(def ^:private valid-content-types
  #{"text/xml" "application/xml"})

(defn s3-loader
  "Create version loader for S3 repository."
  [bucket & [{:keys [path username passphrase]
              :or {path "releases"}}]]
  {:pre [(string? username)
         (string? passphrase)]}
  (let [credentials {:access-key username
                     :secret-key passphrase}
        get! #(s3/get-object credentials bucket %)]
    (fn [group id]
      (try
        (let [object-id (xml/metadata-uri path group id)
              {:keys [content metadata]} (get! object-id)
              {:keys [content-type]} metadata]
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
