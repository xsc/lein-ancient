(ns leiningen.ancient.artifact.check-test
  (:require [clojure.test :refer :all]
            [leiningen.ancient.artifact
             [check :refer :all]
             [options :as o]]))

(let [data '{:dependencies [[group/artifact "1.0.0"]
                            [group/excluded "1.0.0" :upgrade false]
                            [org.clojure/clojure "1.5.1"]
                            [org.clojure/clojurescript "1.7.58"]
                            [snapshot "SNAPSHOT"]
                            [release  "RELEASE"]
                            [latest   "LATEST"]]
             :managed-dependencies [[managed "1.0.0"]]
             :plugins [[group/plugin "0.1.0"]]
             :profiles {:xyz {:dependencies [[artifact "1.2.3"]]
                              :plugins [[plugin "3.2.1"]]}
                        :comp [:xyz {:dependencies [[comp "1.2.3"]]}]}}]
  (deftest t-artifact-collection
    (are [?opts ?artifacts]
         (let [opts (o/options ?opts)
               artifacts (filter :include? (collect-artifacts opts data))
               symbols (map (comp :symbol :artifact) artifacts)
               paths (map :path artifacts)]
           (is (= (set ?artifacts) (set symbols)))
           (is (every? vector? paths))
           (is (every? (complement empty?) paths)))
         {}                                     '[group/artifact artifact comp managed]
         {:check-clojure? true}                 '[group/artifact artifact comp managed org.clojure/clojure org.clojure/clojurescript]
         {:profiles? false}                     '[group/artifact managed]
         {:dependencies? false}                 '[]
         {:plugins? true}                       '[group/artifact artifact comp managed group/plugin plugin]
         {:plugins? true, :profiles? false}     '[group/artifact managed group/plugin]
         {:plugins? true, :dependencies? false} '[group/plugin plugin]
         {:plugins? true
          :dependencies? true
          :check-clojure? true
          :profiles? false}                     '[group/artifact group/plugin managed org.clojure/clojure org.clojure/clojurescript])))

(let [data '{:dependencies [[managed]
                            [my-artifact "1.0.0"]
                            [my-artifact "2.0.0"]]
             :managed-dependencies [[managed "3.0.0"]]}]
  (tabular
    (fact "about artifact collection with duplicate artifacts."
          (let [opts (o/options ?opts)
                artifacts (filter :include? (collect-artifacts opts data))
                info (->> artifacts
                          (map :artifact)
                          (map  #(select-keys % [:symbol :version-string])))]
            (set info) => (set ?info)))
    ?opts                                  ?info
    {}                                     '[{:symbol managed :version-string "3.0.0"} {:symbol my-artifact :version-string "2.0.0"}]))

(let [data '{:dependencies [[snapshot "0-SNAPSHOT"]
                            [qualified "0-alpha"]
                            [release "0"]]
             :plugins [[snapshot-plugin "0-SNAPSHOT"]
                       [qualified-plugin "0-alpha"]
                       [plugin "0"]]}]
  (deftest t-selective-artifact-collection
    (are [?opts ?artifacts]
         (let [opts (o/options ?opts)
               artifacts (collect-artifacts opts data)]
           (is (= (set ?artifacts) (set (map (comp :symbol :artifact) artifacts)))))
         {:only [:qualified]}               '[qualified]
         {:only [:dependencies :qualified]} '[qualified]
         {:only [:plugins :qualified]}      '[]
         {:plugins? true
          :only [:plugins :qualified]}      '[qualified-plugin]
         {:plugins? true
          :only [:qualified]}               '[qualified qualified-plugin]
         {:dependencies? false
          :plugins? true
          :only [:qualified]}               '[qualified-plugin]
         {:exclude [:qualified]}            '[snapshot release]
         {:only [:dependencies]
          :exclude [:qualified]}            '[snapshot release]
         {:only [:snapshots]}               '[snapshot]
         {:plugins? true,
          :only [:snapshots]}               '[snapshot snapshot-plugin]
         {:exclude [:snapshots]}            '[qualified release]
         {:plugins? true
          :exclude [:snapshots]}            '[qualified release qualified-plugin plugin])))

(let [artifact (read-artifact [:path] '[artifact "0.1.0" :exclusions []])
      const-opts #(o/options
                    {:repositories
                     {"const" (constantly [%])}})]
  (deftest t-artifact-analysis
    (is (= [:path] (:path artifact)))
    (is (= '[artifact "0.1.0"] (-> artifact :artifact :form)))
    (is (= 'artifact (-> artifact :artifact :symbol))))
  (deftest t-artifact-checking
    (is (nil? (check-artifact! (const-opts "0.1.0") artifact)))
    (let [{:keys [latest]} (check-artifact! (const-opts "0.1.1") artifact)]
      (is (map? latest))
      (is (= "0.1.1" (:version-string latest))))))

(deftest t-artifact-collection-with-duplicates
  (let [data '{:dependencies [[group/artifact "1.0.0"]
                              [org.clojure/clojure "1.5.1"]
                              [head1 "0.1.0"]
                              [group/artifact "1.0.0"]
                              [rest1 "0.1.0"]
                              [rest2 "0.2.0"]]}
        opts (o/options {})
        artifacts (->> (collect-artifacts opts data)
                       (filter :include?)
                       (map (juxt (comp :symbol :artifact) :path))
                       (set))]
    (is (= (set '[[group/artifact [:dependencies 0]]
                  [head1 [:dependencies 2]]
                  [group/artifact [:dependencies 3]]
                  [rest1 [:dependencies 4]]
                  [rest2 [:dependencies 5]]])
           artifacts))))
