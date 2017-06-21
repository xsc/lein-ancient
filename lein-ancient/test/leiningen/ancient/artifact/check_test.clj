(ns leiningen.ancient.artifact.check-test
  (:require [midje.sweet :refer :all]
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
  (tabular
    (fact "about artifact collection."
          (let [opts (o/options ?opts)
                artifacts (filter :include? (collect-artifacts opts data))
                symbols (map (comp :symbol :artifact) artifacts)
                paths (map :path artifacts)]
            (set symbols) => (set ?artifacts)
            paths => (has every? vector?)
            paths => (has every? (complement empty?))))
    ?opts                                  ?artifacts
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
     :profiles? false}                     '[group/artifact group/plugin managed org.clojure/clojure org.clojure/clojurescript]))

(let [data '{:dependencies [[snapshot "0-SNAPSHOT"]
                            [qualified "0-alpha"]
                            [release "0"]]
             :plugins [[snapshot-plugin "0-SNAPSHOT"]
                       [qualified-plugin "0-alpha"]
                       [plugin "0"]]}]
  (tabular
    (fact "about selective artifact collection."
          (let [opts (o/options ?opts)
                artifacts (collect-artifacts opts data)]
            (set (map (comp :symbol :artifact) artifacts)) => (set ?artifacts)))
    ?opts                              ?artifacts
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
     :exclude [:snapshots]}            '[qualified release qualified-plugin plugin]))

(let [artifact (read-artifact [:path] '[artifact "0.1.0" :exclusions []])
      const-opts #(o/options
                    {:repositories
                     {"const" (constantly [%])}})]
  (fact "about artifact analysis."
        (:path artifact) => [:path]
        (-> artifact :artifact :form) => '[artifact "0.1.0"]
        (-> artifact :artifact :symbol) => 'artifact)
  (fact "about artifact checking."
        (check-artifact! (const-opts "0.1.0") artifact) => nil?
        (let [{:keys [latest]} (check-artifact! (const-opts "0.1.1") artifact)]
          latest => map?
          (:version-string latest) => "0.1.1")))

(fact "about artifact collection with duplicates."
      (let [data '{:dependencies [[group/artifact "1.0.0"]
                                  [org.clojure/clojure "1.5.1"]
                                  [head1 "0.1.0"]
                                  [group/artifact "1.0.0"]
                                  [rest1 "0.1.0"]
                                  [rest2 "0.2.0"]]}
            opts (o/options {})
            artifacts (->> (collect-artifacts opts data)
                           (filter :include?)
                           (map (juxt (comp :symbol :artifact) :path)))]
        (count artifacts) => 5
        artifacts => (contains #{'[group/artifact [:dependencies 0]]})
        artifacts => (contains #{'[head1 [:dependencies 2]]})
        artifacts => (contains #{'[group/artifact [:dependencies 3]]})
        artifacts => (contains #{'[rest1 [:dependencies 4]]})
        artifacts => (contains #{'[rest2 [:dependencies 5]]})))
