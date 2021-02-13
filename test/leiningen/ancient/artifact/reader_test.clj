(ns leiningen.ancient.artifact.reader-test
  (:require [clojure.test :refer :all]
            [leiningen.ancient.utils :refer [with-temp-file]]
            [leiningen.ancient.artifact
             [reader :refer :all]]
            [clojure.java.io :as io])
  (:import [java.io File]))

;; ## Tests

(deftest t-project-file-parsing
  (with-temp-file [f (str "(defproject project-x \"0.1.1-SNAPSHOT\"\n"
                          "  :dependencies [[artifact \"0.1.0\"]]\n"
                          "  :managed-dependencies [[managed-artifact \"0.1.0\"]]\n"
                          "  :plugins [[plugin \"0.1.0\"]]\n"
                          "  :profiles {:dev {:plugins [[plugin2 \"0.1.1\"]]\n"
                          "                   :dependencies [[artifact2 \"0.1.1\"]]}})")]
    (let [m (read-project-map! f)]
      (is (map? m))
      (is (= '[[managed-artifact "0.1.0"]] (:managed-dependencies m)))
      (is (= '[[artifact "0.1.0"]] (:dependencies m)))
      (is (= '[[plugin "0.1.0"]] (:plugins m)))
      (is (= '[[artifact2 "0.1.1"]] (-> m :profiles :dev :dependencies)))
      (is (= '[[plugin2 "0.1.1"]] (-> m :profiles :dev :plugins))))))

(deftest t-profiles-file-parsing
  (with-temp-file [f "{:prof {:plugins [[plugin \"0.1.0\"]]}}"]
    (let [m (read-profiles-map! f [:profiles])]
      (is (= '[[plugin "0.1.0"]] (-> m :profiles :prof :plugins)))))
  (with-temp-file [f "{:plugins [[plugin \"0.1.0\"]]}"]
    (let [m (read-profiles-map! f [:profiles :prof])]
      (is (= '[[plugin "0.1.0"]] (-> m :profiles :prof :plugins))))))

(deftest t-project-file-parsing-for-tests
  (with-temp-file [new-project (str "(defproject project-x \"a\"\n"
                                    "  :dependencies [[artifact \"0.1.1\"]])")]
    (with-temp-file [old-project (str "(defproject project-x \"a\"\n"
                                      "  :dependencies [[artifact \"0.1.0\"]])")]
      (let [parent-path #(.getCanonicalPath (.getParentFile (io/file %)))
            m (read-project-for-tests! (parent-path old-project) new-project)
            {:keys [without-profiles]} (meta m)]
        (is (contains? (set (:dependencies m)) '[artifact/artifact "0.1.1"]))
        (is (contains? (set (:dependencies without-profiles)) '[artifact/artifact "0.1.1"]))
        (is (= (parent-path old-project) (-> without-profiles :root parent-path)))))))

;; https://github.com/technomancy/leiningen/blob/master/doc/PROFILES.md#composite-profiles
(deftest t-project-file-with-composite-profile-parsing
  (with-temp-file [f (str "(defproject project-x \"0.1.1-SNAPSHOT\"\n"
                          "  :dependencies [[artifact \"0.1.0\"]]\n"
                          "  :plugins [[plugin \"0.1.0\"]]\n"
                          "  :profiles {:shared {:plugins [[plugin2 \"0.1.1\"]]\n"
                          "                      :dependencies [[artifact2 \"0.1.1\"]]}\n"
                          "             :dev [:shared {:plugins [[plugin3 \"0.1.2\"]]\n"
                          "                            :dependencies [[artifact3 \"0.1.2\"]]}]})")]
    (let [m (read-project-map! f)]
      (is (map? m))
      (is (= '[[artifact "0.1.0"]] (:dependencies m)))
      (is (= '[[plugin "0.1.0"]] (:plugins m)))
      (is (= '[[artifact2 "0.1.1"]] (-> m :profiles :shared :dependencies)))
      (is (= '[[plugin2 "0.1.1"]] (-> m :profiles :shared :plugins)))
      (is (= :shared (-> m :profiles :dev first)))
      (is (= '[[artifact3 "0.1.2"]] (-> m :profiles :dev second :dependencies)))
      (is (= '[[plugin3 "0.1.2"]] (-> m :profiles :dev second :plugins))))))
