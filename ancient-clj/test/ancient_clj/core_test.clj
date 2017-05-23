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

(def releases
  (v/version-sort xml/release-versions))

(def qualified
  (v/version-sort xml/qualified-versions))

(def snapshots
  (v/version-sort xml/snapshot-versions))

(defn no-duplicates?
  [sq]
  (= (count (set sq)) (count sq)))

;; ## Tests

(tabular
  (tabular
    (tabular
      (let [opts (-> (merge ?sort-opts ?snapshots-opts ?qualified-opts)
                     (assoc :repositories repositories))
            all (-> releases
                    (concat ?qualified ?snapshots)
                    (v/version-sort))]
        (facts "about artifact version retrieval"
               (fact "about versions-per-repository!"
                 (let [vs (versions-per-repository! 'pandect opts)
                       vss (apply concat (vals vs))]
                   vss => (has every? :version)
                   vss => (has every? :version-string)
                   vss =not=> no-duplicates?
                   (version-strings vs "all") => (?f all)
                   (version-strings vs "releases")  => (?f releases)
                   (version-strings vs "qualified") => (?f ?qualified)
                   (version-strings vs "snapshots") => (?f ?snapshots)))
               (fact "about versions!"
                 (let [vs (versions! 'pandect opts)]
                   vs => (has every? :version)
                   vs => (has every? :version-string)
                   vs => no-duplicates?
                   (map :version-string vs) => (?f all)))
               (fact "about version-strings!"
                 (let [vs (version-strings! 'pandect opts)]
                   vs => (has every? string?)
                   vs => no-duplicates?
                   vs => (?f all)))
               (fact "about latest-version!"
                 (let [v (latest-version! 'pandect opts)]
                   (:version v) => truthy
                   (:version-string v) => (last all)))
               (fact "about latest-version-string!"
                 (latest-version-string! 'pandect opts) => (last all))
               (fact "about artifact-outdated?"
                 (let [v (artifact-outdated? '[pandect (first all)] opts)]
                   (:version v) => truthy
                   (:version-string v) => (last all)))
               (fact "about artifact-outdated-string?"
                 (let [v (artifact-outdated-string? '[pandect (first all)] opts)]
                   v => (last all)))))
      ?sort-opts        ?f
      {:sort :desc}     reverse
      {:sort :asc}      identity)
    ?snapshots-opts     ?snapshots
    {:snapshots? true}  snapshots
    {:snapshots? false} [])
  ?qualified-opts           ?qualified
  {:qualified? true}        qualified
  {:qualified? false}       [])
