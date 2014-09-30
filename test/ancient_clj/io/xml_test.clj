(ns ancient-clj.io.xml-test
  (:require [midje.sweet :refer :all]
            [ancient-clj.io.xml :refer :all]
            [clojure.string :as string]))

;; ## Fixtures

(def snapshot-versions
  ["0.1.0-SNAPSHOT" "0.1.1-SNAPSHOT" "0.1.3-SNAPSHOT"])

(def qualified-versions
  ["0.1.1-RC0" "0.1.3-alpha"])

(def release-versions
  ["0.1.0" "0.1.1" "0.1.2"])

(def versions
  (concat
    snapshot-versions
    qualified-versions
    release-versions))

(defn generate-xml
  []
  (format
    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>
     <metadata>
     <groupId>group</groupId>
     <artifactId>group</artifactId>
     <versioning>
     <release>%s</release>
     <versions>%s</versions>
     <lastUpdated>20140930173943</lastUpdated>
     </versioning>
     </metadata>"
    (last release-versions)
    (->> (for [v (shuffle versions)]
           (format "<version>%s</version>" v))
         (string/join "\n"))))

;; ## Tests

(fact "about XML version collection"
      (let [vs (-> (generate-xml)
                   (metadata-xml->versions)
                   (set))]
        vs => (has every? string?)
        (count vs) => (count versions)
        snapshot-versions => (has every? vs)
        qualified-versions => (has every? vs)
        release-versions => (has every? vs)))

(tabular
  (fact "about path creation."
        (let [p (metadata-path ?group ?id ?filename)
              u (metadata-uri "http://nexus.me/" ?group ?id ?filename)]
           p => ?path
           u => (str "http://nexus.me/" p)))
  ?group        ?id         ?filename        ?path
  "pandect"     "pandect"   nil              "pandect/pandect/maven-metadata.xml"
  "pandect"     "pandect"   "xy.xml"         "pandect/pandect/xy.xml"
  "org.clojure" "clojure"   nil              "org/clojure/clojure/maven-metadata.xml"
  "org.clojure" "clojure"   "xy.xml"         "org/clojure/clojure/xy.xml"
  "some"        "my.pkg"    nil              "some/my.pkg/maven-metadata.xml"
  "some"        "my.pkg"    "xy.xml"         "some/my.pkg/xy.xml"
  "org.clojure" "data.json" nil              "org/clojure/data.json/maven-metadata.xml"
  "org.clojure" "data.json" "xy.xml"         "org/clojure/data.json/xy.xml")
