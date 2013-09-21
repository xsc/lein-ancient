(ns leiningen.cli-test
  (:require [midje.sweet :refer :all]
            [leiningen.ancient.utils.cli :refer :all]))

(fact "about the default settings"
  (parse-cli nil) => 
      {:aggressive?      false
       :check-clojure    false
       :dependencies     true
       :interactive      false
       :no-colors        false
       :overwrite-backup false
       :plugins          false
       :print            false
       :profiles         true
       :qualified?       false
       :snapshots?       false
       :tests            true
       :verbose          false})
