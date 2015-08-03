(ns leiningen.ancient.artifact.files-test
  (:require [midje.sweet :refer :all]
            [leiningen.ancient.utils :refer [with-temp-file]]
            [leiningen.ancient.artifact
             [files :refer :all]
             [options :as o]])
  (:import [java.io StringWriter]))

(defn- const-opts
  [v]
  (o/options
    {:plugins? true
     :repositories
     {"const" (constantly [v])}}))

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
                    (write-string! u) => expected
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
  #(profiles-file % [:profiles :prof]))

(fact "about project file upgrading failure because of modifications."
        (let [opts (const-opts "0.1.1")
              contents (str "(defproject project-x \"0.1.1-SNAPSHOT\"\n"
                            "  :dependencies [[artifact \"0.1.0\"]])")]
          (with-temp-file [tmp contents]
            (let [f (read! (project-file tmp))
                  outdated (check! f opts)
                  upgraded (upgrade! f outdated)]
              (count outdated) =>  1
              (doto ^java.io.File tmp
                (spit (str contents "\n\n;; EOF"))
                (.setLastModified (+ (System/currentTimeMillis) 5000)))
              (write-string! upgraded) =not=> empty?
             (write-out! upgraded) => #(instance? Throwable %)))))

(fact "about project file upgrading with :exclusions."
      (let [opts (const-opts "0.1.1")
            mk #(format
                  (str "(defproject project-x \"0.1.1-SNAPSHOT\"\n"
                       "  :dependencies [[artifact %s :exclusions [other]]])")
                  (pr-str %))]
        (with-temp-file [tmp (mk "0.1.0")]
          (let [f (read! (project-file tmp))
                outdated (check! f opts)
                artifacts (map (juxt (comp :symbol :artifact) :path) outdated)
                upgraded (upgrade! f outdated)
                expected (mk "0.1.1")]
            (count outdated) => 1
            artifacts => (contains #{'[artifact  [:dependencies 0]]})
            (write-string! upgraded) => expected))))

(fact "about project file upgrading with multiple identical artifacts."
      (let [opts (const-opts "0.1.1")
            mk #(format
                  (str "(defproject project-x \"0.1.1-SNAPSHOT\"\n"
                       "  :dependencies [[artifact %s]\n"
                       "                 [other \"0.1.5\"]\n"
                       "                 [artifact %s]\n"
                       "                 [artifact2 %s]\n"
                       "                 [other2 \"0.1.3\"]])")
                  (pr-str %)
                  (pr-str %)
                  (pr-str %))]
        (with-temp-file [tmp (mk "0.1.0")]
          (let [f (read! (project-file tmp))
                outdated (check! f opts)
                artifacts (map (juxt (comp :symbol :artifact) :path) outdated)
                upgraded (upgrade! f outdated)
                expected (mk "0.1.1")]
            (count outdated) => 3
            artifacts => (contains #{'[artifact  [:dependencies 0]]})
            artifacts => (contains #{'[artifact  [:dependencies 2]]})
            artifacts => (contains #{'[artifact2 [:dependencies 3]]})
            (write-string! upgraded) => expected))))
