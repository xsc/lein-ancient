(ns ^{ :doc "Test for Core API"
       :author "Yannick Scherer" }
  ancient-clj.core-test
  (:require [midje.sweet :refer :all]
            [ancient-clj.core :as r]
            [version-clj.core :as v]))

(tabular
  (fact "about artifact map creation"
    (let [[_ v :as a] ?artifact-vector
          m (r/artifact-map a)]
      (:group-id m) => ?group
      (:artifact-id m) => ?artifact
      (:version m) => [v (v/version->seq v)]))
  ?artifact-vector                ?group         ?artifact
  '[pandect "0.3.0"]              "pandect"      "pandect"
  '[org.clojure/clojure "1.5.1"]  "org.clojure"  "clojure")
