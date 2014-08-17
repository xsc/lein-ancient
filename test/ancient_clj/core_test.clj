(ns ^{ :doc "Test for Core API"
       :author "Yannick Scherer" }
  ancient-clj.core-test
  (:require [midje.sweet :refer :all]
            [ancient-clj.core :as r]
            [version-clj.core :as v]))

(tabular
  (fact "about artifact map creation"
    (let [a ?artifact-vector
          m (r/artifact-map a)]
      (:group-id m) => ?group
      (:artifact-id m) => ?artifact
      (:version m) => [?v (v/version->seq ?v)]))
  ?artifact-vector                ?group         ?artifact      ?v
  '[pandect "0.3.0"]              "pandect"      "pandect"      "0.3.0"
  '[org.clojure/clojure "1.5.1"]  "org.clojure"  "clojure"      "1.5.1"
  '[ancient-clj ""]               "ancient-clj"  "ancient-clj"  ""
  '[ancient-clj]                  "ancient-clj"  "ancient-clj"  ""
  'org.clojure/clojure            "org.clojure"  "clojure"      ""
  'ancient-clj                    "ancient-clj"  "ancient-clj"  "")
