(ns ancient-clj.io.local-test
  (:require [midje.sweet :refer :all]
            [ancient-clj.io
             [local :refer [local-loader]]
             [xml :refer [metadata-path]]
             [xml-test :as xml]]
            [clojure.java.io :as io])
  (:import [java.io File]))

(let [paths (atom [])]
  (with-state-changes [(before :facts (let [dir (doto (File/createTempFile "ancient" "repo")
                                                  (.delete))
                                            f (io/file dir (metadata-path "group" "id"))
                                            f' (->> (metadata-path
                                                      "group" "other"
                                                      "maven-metadata-local.xml")
                                                    (io/file dir))]
                                        (doto (.getParentFile f)
                                          (.mkdirs))
                                        (doto (.getParentFile f')
                                          (.mkdirs))
                                        (spit f (xml/generate-xml))
                                        (spit f' (xml/generate-xml))
                                        (->> (iterate #(.getParentFile %) f)
                                             (take 4)
                                             (cons f')
                                             (reverse)
                                             (reset! paths))))
                       (after :facts (doseq [f (reverse @paths)]
                                       (.delete f)))]
    (tabular
      (fact "about the local XML version loader"
          (let [loader (local-loader (first @paths))
                vs (set (loader "group" ?id))]
            vs => (has every? string?)
            (count vs) => (count xml/versions)
            xml/snapshot-versions => (has every? vs)
            xml/qualified-versions => (has every? vs)
            xml/release-versions => (has every? vs)))
      ?id
      "id"
      "other")
    (fact "about missing artifact"
          (let [loader (local-loader (first @paths))]
            (loader "group" "invalid") => empty?))))


