(ns leiningen.version-test
  (:use midje.sweet
        leiningen.ancient.version))

(tabular
  (fact "about version map creation"
    (let [m (version-map ?version)]
      (:version m) => ?v
      (:version-str m) => ?version))
  ?version         ?v
  "1.0.0"          [[1 0 0]]
  "1.0"            [[1 0]]
  "1"              [[1]]
  "1a"             [[1] ["a"]]
  "1-a"            [[1] ["a"]]
  "1.0.1-SNAPSHOT" [[1 0 1] ["snapshot"]]
  "1.0.1-alpha2"   [[1 0 1] ["alpha" 2]]
  "11.2.0.3.0"     [[11 2 0 3 0]])

(fact "about SNAPSHOTs"
  (version-map "1.0.0") =not=> snapshot?
  (version-map "1.0.0-SNAPSHOT") => snapshot?)

(fact "about qualified versions"
  (version-map "1.0.0") =not=> qualified?
  (version-map "1.0.0-SNAPSHOT") => qualified?
  (version-map "1.0.0-alpha1") => qualified?
  (version-map "1.0.0-1-2") =not=> qualified?
  (version-map "1.0.0-1-2-coala") => qualified?)
