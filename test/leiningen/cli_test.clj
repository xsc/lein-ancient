(ns leiningen.cli-test
  (:require [midje.sweet :refer :all]
            [leiningen.ancient.utils.cli :refer :all]))

(def DEFAULTS (parse-cli nil))

(fact "about the default settings"
  DEFAULTS => 
      {:aggressive?      false
       :check-clojure    false
       :dependencies     true
       :interactive      false
       :no-colors        false
       :overwrite-backup false
       :plugins          false
       :print            false
       :profiles         true
       :recursive        false
       :qualified?       false
       :snapshots?       false
       :tests            true})

(tabular
  (fact "about flag effects"
    (parse-cli ?args) => (apply assoc DEFAULTS ?effects))
  ?args                     ?effects
  [":plugins"]              [:dependencies false :plugins true]
  [":all"]                  [:dependencies true  :plugins true]
  [":interactive"]          [:interactive true]
  [":allow-snapshots"
   ":allow-qualified"]      [:snapshots? true :qualified? true]
  [":allow-all"]            [:snapshots? true :qualified? true]
  [":aggressive"]           [:aggressive? true]
  [":recursive"]            [:recursive true]
  [":overwrite-backup"]     [:overwrite-backup true]
  [":no-tests"]             [:tests false]
  [":no-profiles"]          [:profiles false]
  [":no-colors"]            [:no-colors true]
  [":check-clojure"
   ":print"]                [:check-clojure true :print true])
