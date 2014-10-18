(ns leiningen.ancient.artifact.files-test
  (:require [midje.sweet :refer :all]
            [leiningen.ancient.artifact
             [files :refer :all]
             [options :as o]
             [reader-test :refer :all]])
  (:import [java.io StringWriter]))

(let [const-opts #(o/options
                    {:plugins? true
                     :repositories
                     {"const" (constantly [%])}})]
  (tabular
    (tabular
      (fact "about project file upgrading."
            (let [[a _ & rst] ?artifact
                  upgraded (reduce conj [a "0.1.1"] rst)
                  opts (const-opts "0.1.1")
                  contents (format ?fmt (pr-str ?artifact))
                  expected (format ?fmt (pr-str upgraded))]
              (with-temp-file [tmp contents]
                (let [f (?file tmp)
                      r (read! f)]
                  f => #(satisfies? Dependencies %)
                  r => #(satisfies? Dependencies %)
                  (let [outdated (check! r opts)]
                    (count outdated) =>  1
                    outdated => (has every? :latest)
                    (map (comp :version-string :latest) outdated) => (has every? #{"0.1.1"})
                    (let [u (upgrade! r outdated)]
                      u => #(satisfies? Dependencies %)
                      (with-open [w (StringWriter.)]
                        (write! u w)
                        (.toString w) => expected)
                      (write-out! u)
                      (slurp tmp) => expected))))))
      ?artifact
      '[artifact]
      '[artifact "0.1.0"]
      '[artifact "0.1.0" :exclusions [other]])
    ?fmt ?file
    (str "(defproject project-x \"0.1.1-SNAPSHOT\"\n"
         "  :dependencies [%s])")
    project-file

    (str "(defproject project-x \"0.1.1-SNAPSHOT\"\n"
         "  :dependencies [#_[ignore] %s])")
    project-file

    (str "{:prof {:plugins [[xyz \"0.2.0\"]%n"
         "                  %s]}}")
    profiles-file

    (str "{:plugins [[xyz \"0.2.0\"]%n"
         "           %s]}")
    #(profiles-file % [:profiles :prof])))
