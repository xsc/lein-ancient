(ns ^{ :doc "HTTP Repository Handling"
       :author "Yannick Scherer" }
  ancient-clj.repository.http
  (:require [ancient-clj.verbose :refer [verbose]]
            [ancient-clj.repository.core :refer [create-repository build-metadata-url]]
            [aws.sdk.s3 :as s3 :only [get-object]]))

(defmethod create-repository "s3p"
  [{:keys [url username passphrase] :as m}]
  (let [url (.substring ^String url 6)
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
