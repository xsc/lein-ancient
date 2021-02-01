(ns leiningen.ancient.artifact.zip-test
  (:require [clojure.test :refer :all]
            [leiningen.ancient.utils :refer [with-temp-file]]
            [leiningen.ancient.artifact
             [check :as check]
             [zip :refer :all]])
  (:import [java.io StringWriter]))

(deftest t-project-file-upgrading
  (are [?fmt ?reader ?path]
       (are [?artifact]
            (let [artifact (-> (check/read-artifact ?path ?artifact)
                               (assoc :latest {:version-string "0.1.1"}))
                  [a _ & rst] ?artifact
                  upgraded (reduce conj [a "0.1.1"] rst)
                  contents (format ?fmt (pr-str ?artifact))
                  expected (format ?fmt (pr-str upgraded))]
              (with-temp-file [f contents]
                (let [result (with-open [w (StringWriter.)]
                               (-> (?reader f)
                                   (upgrade-artifacts [artifact])
                                   (write-zipper! w))
                               (.toString w))]
                  (is (= expected result)))))
            '[artifact]
            '[artifact "0.1.0"]
            '[artifact "0.1.0" :exclusions [other]])
       (str "(defproject project-x \"0.1.1-SNAPSHOT\"\n"
            "  :dependencies [%s])")
       read-project-zipper!
       [:dependencies 0]

       (str "(defproject project-x \"0.1.1-SNAPSHOT\"\n"
            "  :dependencies #^:replace [[xyz \"0.2.0\"]\n"
            "                            %s\n"
            "                            [abc \"0.3.0\"]])")
       read-project-zipper!
       [:dependencies 1]

       (str "(defproject project-x \"0.1.1-SNAPSHOT\"\n"
            "  :dependencies ^:replace [[xyz \"0.2.0\"]\n"
            "                           %s\n"
            "                           [abc \"0.3.0\"]])")
       read-project-zipper!
       [:dependencies 1]

       (str "(defproject project-x \"0.1.1-SNAPSHOT\"\n"
            "  :dependencies [[xyz \"0.2.0\"]\n"
            "                 ^:some-meta %s\n"
            "                 [abc \"0.3.0\"]])")
       read-project-zipper!
       [:dependencies 1]

       (str "(defproject project-x \"0.1.1-SNAPSHOT\"\n"
            "  :dependencies [#_[ignore] %s])")
       read-project-zipper!
       [:dependencies 0]

       (str "{:prof {:plugins [[xyz \"0.2.0\"]%n"
            "                  %s]}}")
       read-profiles-zipper!
       [:prof :plugins 1]))
