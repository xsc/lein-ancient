(ns leiningen.projects-test
  (:use midje.sweet
        leiningen.ancient.projects
        leiningen.ancient.version))

(tabular
  (fact "about dependency maps"
    (let [d (dependency-map [?dep "1.0.0"])]
      (:group-id d) => ?group
      (:artifact-id d) => ?artifact
      (:version d) => (version-map "1.0.0")))
  ?dep                       ?group         ?artifact
  "test/test"                "test"         "test"
  "group/artifact"           "group"        "artifact"
  "test"                     "test"         "test")

(def test-project
  '{:dependencies [[group/artifact "1.0.0"]
                   [org.clojure/clojure "1.5.1"]]
    :plugins [[group/plugin "0.1.0"]]
    :profiles {:xyz {:dependencies [[xyz "1.2.3"]]
                     :plugins [[xyz-plugin "3.2.1"]]}}})

(tabular
  (fact "about dependency collection"
    (let [deps (collect-dependencies test-project ?settings)]
      (map (juxt :group-id :artifact-id) deps) => (just ?result)))
  ?settings                                                  ?result
  {:dependencies true}                                       [["group" "artifact"] ["xyz" "xyz"]]
  {:plugins true}                                            [["group" "plugin"] ["xyz-plugin" "xyz-plugin"]]
  {:dependencies true :plugins true}                         [["group" "artifact"] ["group" "plugin"] 
                                                              ["xyz" "xyz"] ["xyz-plugin" "xyz-plugin"]]
  {:dependencies true :no-profiles true}                     [["group" "artifact"]]
  {:plugins true :no-profiles true}                          [["group" "plugin"]]
  {:dependencies true :plugins true :no-profiles true}       [["group" "artifact"] ["group" "plugin"]]
  {:dependencies true :check-clojure true}                   [["group" "artifact"] ["org.clojure" "clojure"]
                                                              ["xyz" "xyz"]]
  {:dependencies true :check-clojure true :no-profiles true} [["group" "artifact"] ["org.clojure" "clojure"]])
