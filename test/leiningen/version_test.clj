(ns leiningen.version-test
  (:require [midje.sweet :refer :all]
            [version-clj.core :refer [version->seq]]
            [leiningen.ancient.version :refer :all]))

(tabular
  (fact "about version map creation"
    (let [m (version-map ?version)]
      (:version m) => (version->seq ?version)
      (:version-str m) => ?version))
  ?version         
  "1.0.0"
  "1.0"
  "1"
  "1a"
  "1-a"
  "1.0.1-SNAPSHOT"
  "1.0.1-alpha2"
  "11.2.0.3.0")

;; For more tests and examples see version-clj's unit tests.

(fact "about SNAPSHOTs"
  (version-map "1.0.0") =not=> snapshot?
  (version-map "1.0.0-SNAPSHOT") => snapshot?)

(fact "about qualified versions"
  (version-map "1.0.0") =not=> qualified?
  (version-map "1.0.0-SNAPSHOT") => qualified?
  (version-map "1.0.0-alpha1") => qualified?
  (version-map "1.0.0-1-2") =not=> qualified?
  (version-map "1.0.0-1-2-coala") => qualified?)
