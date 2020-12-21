(ns leiningen.ancient.artifact.files-test
  (:require [clojure.test :refer :all]
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

(deftest t-project-file-upgrading
  (are [?fmt ?file]
       (are [?artifact]
            (let [[a _ & rst] ?artifact
                  upgraded (reduce conj [a "0.1.1"] rst)
                  opts (const-opts "0.1.1")
                  contents (format ?fmt (pr-str ?artifact))
                  expected (format ?fmt (pr-str upgraded))]
              (with-temp-file [tmp contents]
                (let [f (?file tmp)
                      r (read! f)]
                  (is (satisfies? Dependencies f))
                  (is (satisfies? Dependencies r))
                  (let [outdated (check! r opts)]
                    (is (= 1 (count outdated)))
                    (is (every? :latest outdated))
                    (is (every? #{"0.1.1"} (map (comp :version-string :latest) outdated)))
                    (let [u (upgrade! r outdated)]
                      (is (satisfies? Dependencies u))
                      (is (= expected (write-string! u)))
                      (write-out! u)
                      (is (= expected (slurp tmp))))))))
            '[artifact]
            '[artifact "0.1.0"]
            '[artifact "0.1.0" :exclusions [other]])
       (str "(defproject project-x \"0.1.1-SNAPSHOT\"\n"
            "  :dependencies [%s])")
       project-file

       (str "(defproject project-x \"0.1.1-SNAPSHOT\"\n"
            "  :managed-dependencies [%s])")
       project-file

       (str "(defproject project-x \"0.1.1-SNAPSHOT\"\n"
            "  :dependencies         [[artifact]]"
            "  :managed-dependencies [%s])")
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

(deftest t-project-file-upgrading-failure-because-of-modifications
  (let [opts (const-opts "0.1.1")
        contents (str "(defproject project-x \"0.1.1-SNAPSHOT\"\n"
                      "  :dependencies [[artifact \"0.1.0\"]])")]
    (with-temp-file [tmp contents]
      (let [f (read! (project-file tmp))
            outdated (check! f opts)
            upgraded (upgrade! f outdated)]
        (is (= 1 (count outdated)))
        (doto ^java.io.File tmp
          (spit (str contents "\n\n;; EOF"))
          (.setLastModified (+ (System/currentTimeMillis) 5000)))
        (is (seq (write-string! upgraded)))
        (is (instance? Throwable (write-out! upgraded)))))))

(deftest t-project-file-upgrading-with-exclusions
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
        (is (= 1 (count outdated)))
        (is (contains? (set artifacts) (quote [artifact [:dependencies 0]])))
        (is (= expected (write-string! upgraded)))))))

(deftest t-partial-project-file-upgrading
  (let [opts (const-opts "0.1.1")
        mk #(format
              (str "(defproject project-x \"0.1.1-SNAPSHOT\"\n"
                   "  :dependencies [[artifact %s]"
                   "                 [artifact2 %s :upgrade? false]"
                   "                 [artifact3 %s :upgrade? true]])")
              (pr-str %1)
              (pr-str %2)
              (pr-str %1))]
    (with-temp-file [tmp (mk "0.1.0" "0.1.0")]
      (let [f (read! (project-file tmp))
            outdated (check! f opts)
            artifacts (map (juxt (comp :symbol :artifact) :path) outdated)
            upgraded (upgrade! f outdated)
            expected (mk "0.1.1" "0.1.0")]
        (is (= 2 (count outdated)))
        (is (contains? (set artifacts) (quote [artifact [:dependencies 0]])))
        (is (not (contains? (set artifacts) (quote [artifact2 [:dependencies 1]]))))
        (is (contains? (set artifacts) (quote [artifact3 [:dependencies 2]])))
        (is (= expected (write-string! upgraded)))))))

(deftest t-project-file-upgrading-with-multiple-identical-artifacts
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
        (is (= 3 (count outdated)))
        (is (contains? (set artifacts) (quote [artifact [:dependencies 0]])))
        (is (contains? (set artifacts) (quote [artifact [:dependencies 2]])))
        (is (contains? (set artifacts) (quote [artifact2 [:dependencies 3]])))
        (is (= expected (write-string! upgraded)))))))

(deftest t-project-file-upgrading-with-profiles
  (let [opts (const-opts "0.1.1")
        mk #(format
              (str "(defproject project-x \"0.1.1-SNAPSHOT\"\n"
                   "  :dependencies [[artifact %s]\n"
                   "                 [other \"0.1.5\"]\n"
                   "                 [other2 \"0.1.3\"]]\n"
                   "  :profiles {:dev {:dependencies [[artifact2 %s]]}\n"
                   "             :build [:dev\n"
                   "                     {:dependencies [[artifact3 %s]]}]})")
              (pr-str %)
              (pr-str %)
              (pr-str %))]
    (with-temp-file [tmp (mk "0.1.0")]
      (let [f (read! (project-file tmp))
            outdated (check! f opts)
            artifacts (map (juxt (comp :symbol :artifact) :path) outdated)
            upgraded (upgrade! f outdated)
            expected (mk "0.1.1")]
        (is (= 3 (count outdated)))
        (is (contains? (set artifacts) (quote [artifact [:dependencies 0]])))
        (is (contains? (set artifacts) (quote [artifact2 [:profiles :dev :dependencies 0]])))
        (is (contains? (set artifacts) (quote [artifact3 [:profiles :build 1 :dependencies 0]])))
        (is (= expected (write-string! upgraded)))))))
