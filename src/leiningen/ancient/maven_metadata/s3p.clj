(ns ^{ :doc "Amazon S3 Repositories"
       :author "Yannick Scherer" }
  leiningen.ancient.maven-metadata.s3p
  (:require [leiningen.core.user :as uu]
            [aws.sdk.s3 :as s3])
  (:use [leiningen.ancient.maven-metadata :only [metadata-retriever]]
        [leiningen.ancient.maven-metadata.utils :only [build-metadata-url]]
        [leiningen.ancient.verbose :only [verbose]]))

(defmethod metadata-retriever "s3p" [m]
  (let [{:keys [url username passphrase]} (uu/resolve-credentials m)
        url (.substring ^String url 6)
        [bucket key-prefix] (.split ^String url "/" 2) 
        creds { :access-key username :secret-key passphrase }
        get! (partial s3/get-object creds bucket)]
    (when-not (or (= bucket "") (not key-prefix) (= key-prefix ""))
      (fn [group-id artifact-id]
        (let [k (build-metadata-url key-prefix group-id artifact-id)]
          (verbose "  Trying to retrieve " k " (S3 bucket: " bucket ") ...")
          (let [{:keys [content]} (get! k)]
            (when-let [xml (slurp content)]
              (verbose "  Got " (count xml) " byte(s) of data.")
              xml)))))))

