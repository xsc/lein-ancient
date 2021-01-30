(ns ancient-clj.io.google-cloud-storage-test
  (:require [midje.sweet :refer :all]
            [ancient-clj.io
             [google-cloud-storage :refer [google-cloud-storage-loader]]
             [xml :refer [metadata-uri]]
             [xml-test :as xml]])
  (:import [com.google.cloud.storage StorageException]))

;; ## Fixtures

(def opts
  {:path "snapshots"})

;; ## Tests
(against-background
  [(#'ancient-clj.io.google-cloud-storage/get-object!
     anything
     "bucket"
     (metadata-uri "snapshots" "group" "id"))
   => {:content (.getBytes (xml/generate-xml) "UTF-8")
       :content-type "text/xml"}]
  (fact "about the Google Cloud Storage/XML version loader."
        (let [loader (google-cloud-storage-loader "bucket" opts)
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
      [(#'ancient-clj.io.google-cloud-storage/get-object!
         anything
         "bucket"
         (metadata-uri "snapshots" "group" "id"))
       => ?object]
      (fact "about Google Cloud Storage/XML version loader failures."
            (let [loader (google-cloud-storage-loader "bucket" opts)]
              (loader "group" "id") => ?check)))
    ?object                          ?check
    {}                               []
    {:content-type "text/plain"}     (throwable? "content-type is not XML")
    {:content-type "text/xml;a=b"}   (throwable? "content not found")
    {:content-type "text/xml"}       (throwable? "content not found")
    {:content (.getBytes "<not-xml>" "UTF-8")
     :content-type "text/xml"}       (throwable? "Could not parse metadata XML"))
  (fact "about handling Google Cloud Storage errors."
        (with-redefs [ancient-clj.io.google-cloud-storage/get-object!
                      (fn [& _]
                        (throw (StorageException. 403 "InvalidAccessKey")))]
          (let [loader (google-cloud-storage-loader "bucket" opts)]
            (loader "group" "id") => (throwable? "[code=403] InvalidAccessKey")))))
