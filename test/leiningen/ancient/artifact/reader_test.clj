(ns leiningen.ancient.artifact.reader-test
  (:require [midje.sweet :refer :all]
            [leiningen.ancient.utils :refer [with-temp-file]]
            [leiningen.ancient.artifact
             [reader :refer :all]]
            [clojure.java.io :as io])
  (:import [java.io File]))

;; ## Tests

(fact "about project file parsing."
      (with-temp-file [f (str "(defproject project-x \"0.1.1-SNAPSHOT\"\n"
                              "  :dependencies [[artifact \"0.1.0\"]]\n"
                              "  :plugins [[plugin \"0.1.0\"]]\n"
                              "  :profiles {:dev {:plugins [[plugin2 \"0.1.1\"]]\n"
                              "                   :dependencies [[artifact2 \"0.1.1\"]]}})")]
        (let [m (read-project-map! f)]
          m => map?
          (:dependencies m) => '[[artifact "0.1.0"]]
          (:plugins m) => '[[plugin "0.1.0"]]
          (-> m :profiles :dev :dependencies) => '[[artifact2 "0.1.1"]]
          (-> m :profiles :dev :plugins) => '[[plugin2 "0.1.1"]])))

(fact "about profiles file parsing."
      (with-temp-file [f "{:prof {:plugins [[plugin \"0.1.0\"]]}}"]
        (let [m (read-profiles-map! f [:profiles])]
          (-> m :profiles :prof :plugins) => '[[plugin "0.1.0"]]))
      (with-temp-file [f "{:plugins [[plugin \"0.1.0\"]]}"]
        (let [m (read-profiles-map! f [:profiles :prof])]
          (-> m :profiles :prof :plugins) => '[[plugin "0.1.0"]])))

(fact "about project file parsing (for tests)."
      (with-temp-file [new-project (str "(defproject project-x \"a\"\n"
                                        "  :dependencies [[artifact \"0.1.1\"]])")]
        (with-temp-file [old-project (str "(defproject project-x \"a\"\n"
                                          "  :dependencies [[artifact \"0.1.0\"]])")]
          (let [parent-path #(.getCanonicalPath (.getParentFile (io/file %)))
                m (read-project-for-tests! (parent-path old-project) new-project)
                {:keys [without-profiles]} (meta m)]
            (:dependencies m) => (contains #{'[artifact/artifact "0.1.1"]})
            (:dependencies without-profiles) => (contains #{'[artifact/artifact "0.1.1"]})
            (-> without-profiles :root parent-path) => (parent-path old-project)))))