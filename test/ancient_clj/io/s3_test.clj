(ns ancient-clj.io.s3-test
  (:require [midje.sweet :refer :all]
            [ancient-clj.io
             [s3 :refer [s3-loader]]
             [xml :refer [metadata-uri]]
             [xml-test :as xml]]
            [clojure.set :refer [rename-keys]])
  (:import [com.amazonaws.services.s3.model AmazonS3Exception]))

;; ## Fixtures

(def opts
  {:username   "abc"
   :passphrase "def"
   :path       "snapshots"})

(def aws-creds
  (-> opts
      (select-keys [:username :passphrase])
      (rename-keys
        {:username :access-key
         :passphrase :secret-key})))

;; ## Tests

(against-background
  [(aws.sdk.s3/get-object
     aws-creds
     "bucket"
     (metadata-uri "snapshots" "group" "id"))
   => {:content (.getBytes (xml/generate-xml) "UTF-8")
       :metadata {:content-type "text/xml"}}]
  (fact "about the S3/XML version loader."
        (let [loader (s3-loader "bucket" opts)
              vs (set (loader "group" "id"))]
          vs => (has every? string?)
          (count vs) => (count xml/versions)
          xml/snapshot-versions => (has every? vs)
          xml/qualified-versions => (has every? vs)
          xml/release-versions => (has every? vs))))

(let [throwable? (fn [msg]
                   (fn [t]
                     (and (instance? Throwable t)
                          (.contains (.getMessage t) msg))))]
  (tabular
    (against-background
      [(aws.sdk.s3/get-object
         aws-creds
         "bucket"
         (metadata-uri "snapshots" "group" "id"))
       => ?object]
      (fact "about S3/XML version loader failures."
            (let [loader (s3-loader "bucket" opts)]
              (loader "group" "id") => ?check)))
    ?object                                     ?check
    {}                                          (throwable? "content-type is not XML")
    {:metadata {:content-type "text/plain"}}    (throwable? "content-type is not XML")
    {:metadata {:content-type "text/xml;a=b"}}  (throwable? "content not found")
    {:metadata {:content-type "text/xml"}}      (throwable? "content not found")
    {:content (.getBytes "<not-xml>" "UTF-8")
     :metadata {:content-type "text/xml"}}      (throwable? "Could not parse metadata XML"))
  (fact "about handling AWS errors."
        (with-redefs [aws.sdk.s3/get-object
                      (fn [& _]
                        (throw
                          (doto (AmazonS3Exception. "oops.")
                            (.setStatusCode 403)
                            (.setErrorCode "InvalidAccessKey"))))]
          (let [loader (s3-loader "bucket" opts)]
            (loader "group" "id") => (throwable? "[status=403] InvalidAccessKey")))))
