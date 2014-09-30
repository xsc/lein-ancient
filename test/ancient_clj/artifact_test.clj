(ns ancient-clj.artifact-test
  (:require [midje.sweet :refer :all]
            [ancient-clj.artifact :refer [read-artifact]]
            [version-clj.core :refer [version->seq]]))

(tabular
  (fact "about artifact representations."
        (let [m (read-artifact (quote ?artifact))]
          (:group m)          => (name (quote ?group))
          (:id m)             => (name (quote ?id))
          (:version-string m) => ?version
          (:version m) => (version->seq ?version)))
  ?artifact                      ?group       ?id         ?version
  [pandect "0.3.0"]              pandect      pandect     "0.3.0"
  [org.clojure/clojure "1.5.1"]  org.clojure  clojure     "1.5.1"
  [pandect]                      pandect      pandect     ""
  [org.clojure/clojure]          org.clojure  clojure     ""
  pandect                        pandect      pandect     ""
  org.clojure/clojure            org.clojure  clojure     ""
  "pandect"                      pandect      pandect     ""
  "org.clojure/clojure"          org.clojure  clojure     ""
  :pandect                       pandect      pandect     ""
  :org.clojure/clojure           org.clojure  clojure     "")
