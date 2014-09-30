(ns ancient-clj.core-test
  (:require [midje.sweet :refer :all]
            [ancient-clj.io.xml-test :as xml]
            [ancient-clj.core :refer :all]
            [version-clj.core :as v]))

;; ## Fixtures

(def repositories
  {"all"       (constantly xml/versions)
   "releases"  (constantly xml/release-versions)
   "qualified" (constantly xml/qualified-versions)
   "snapshots" (constantly xml/snapshot-versions)})

(defn version-strings
  [v k]
  (map :version-string (get v k)))

;; ## Tests

(tabular
  (tabular
    (tabular
      (fact "about versions-per-repository!"
            (let [opts (-> (merge ?sort-opts ?snapshots-opts ?qualified-opts)
                           (assoc :repositories repositories))
                  vs (versions-per-repository! 'pandect opts)
                  vss (apply concat (vals vs))]
              vss => (has every? :version)
              vss => (has every? :version-string)
              (version-strings vs "all") => (-> xml/release-versions
                                                (concat ?qualified ?snapshots)
                                                (v/version-sort)
                                                (?f))
              (version-strings vs "releases")  => (?f (v/version-sort xml/release-versions))
              (version-strings vs "qualified") => (?f (v/version-sort ?qualified))
              (version-strings vs "snapshots") => (?f (v/version-sort ?snapshots))))
      ?sort-opts        ?f
      {}                reverse
      {:sort :desc}     reverse
      {:sort :asc}      identity)
    ?snapshots-opts     ?snapshots
    {}                  xml/snapshot-versions
    {:snapshots? true}  xml/snapshot-versions
    {:snapshots? false} [])
  ?qualified-opts           ?qualified
  {}                        xml/qualified-versions
  {:qualified? true}        xml/qualified-versions
  {:qualified? false}       [])
