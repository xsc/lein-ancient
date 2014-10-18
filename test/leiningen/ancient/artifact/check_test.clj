(ns leiningen.ancient.artifact.check-test
  (:require [midje.sweet :refer :all]
            [leiningen.ancient.artifact
             [check :refer :all]
             [options :as o]]))

(let [data '{:dependencies [[group/artifact "1.0.0"]
                            [org.clojure/clojure "1.5.1"]]
             :plugins [[group/plugin "0.1.0"]]
             :profiles {:xyz {:dependencies [[artifact "1.2.3"]]
                              :plugins [[plugin "3.2.1"]]}}}]
  (tabular
    (fact "about artifact collection."
          (let [opts (o/options ?opts)
                artifacts (collect-artifacts opts data)
                symbols (map (comp :symbol :artifact) artifacts)
                paths (map :path artifacts)]
            (set symbols) => (set ?artifacts)
            paths => (has every? vector?)
            paths => (has every? (complement empty?))))
    ?opts                                  ?artifacts
    {}                                     '[group/artifact artifact]
    {:check-clojure? true}                 '[group/artifact artifact org.clojure/clojure]
    {:profiles? false}                     '[group/artifact]
    {:dependencies? false}                 '[]
    {:plugins? true}                       '[group/artifact artifact group/plugin plugin]
    {:plugins? true, :profiles? false}     '[group/artifact group/plugin]
    {:plugins? true, :dependencies? false} '[group/plugin plugin]
    {:plugins? true
     :dependencies? true
     :check-clojure? true
     :profiles? false}                     '[group/artifact group/plugin org.clojure/clojure]))

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
