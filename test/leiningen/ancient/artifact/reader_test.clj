(ns leiningen.ancient.artifact.reader-test
  (:require [midje.sweet :refer :all]
            [leiningen.ancient.artifact
             [reader :refer :all]]
            [clojure.java.io :as io])
  (:import [java.io File]))

;; ## Helper

(defmacro with-temp-file
  "Create file with the given contents, then run the body."
  [[sym data] & body]
  `(let [f# (File/createTempFile "ancient" ".clj")]
     (try
       (spit f# ~data)
       (let [~sym f#]
         ~@body)
       (finally
         (.delete f#)))))

;; ## Tests

(fact "about project file parsing."
      (with-temp-file [f (str "(defproject project-x \"0.1.1-SNAPSHOT\"\n"
                              "  :dependencies [[artifact \"0.1.0\"]]\n"
                              "  :plugins [[plugin \"0.1.0\"]]\n"
                              "  :profiles {:dev {:dependencies [[artifact2 \"0.1.1\"]]}})")]
        (let [m (read-project-map! f)]
          m => map?
          (:dependencies m) => '[[artifact/artifact "0.1.0"]]
          (:plugins m) => '[[plugin/plugin "0.1.0"]]
          (-> m :profiles :dev :dependencies) '[[artifact2/artifact2 "0.1.1"]])))

(fact "about profiles file parsing."
      (with-temp-file [f "{:prof {:plugins [[plugin \"0.1.0\"]]}}"]
        (let [m (read-profiles-map! f [:profiles])]
          (-> m :profiles :prof :plugins) => '[[plugin "0.1.0"]]))
      (with-temp-file [f "{:plugins [[plugin \"0.1.0\"]]}"]
        (let [m (read-profiles-map! f [:profiles :prof])]
          (-> m :profiles :prof :plugins) => '[[plugin "0.1.0"]])))
