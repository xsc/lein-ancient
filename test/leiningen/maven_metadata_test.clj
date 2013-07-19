(ns leiningen.maven-metadata-test
  (:require [midje.sweet :refer :all]
            [leiningen.ancient.maven-metadata :refer [version-seq latest-version metadata-retriever]]
            [leiningen.ancient.maven-metadata.utils :refer [build-metadata-url]]
            leiningen.ancient.maven-metadata.all))

(fact "about metadata URL creation"
  (build-metadata-url "http://clojars.org/repo" "pandect" "pandect") 
      => "http://clojars.org/repo/pandect/pandect/maven-metadata.xml"
  (build-metadata-url "http://clojars.org/repo" "pandect" "pandect" "maven-metadata-local.xml") 
      => "http://clojars.org/repo/pandect/pandect/maven-metadata-local.xml"
  (build-metadata-url "http://clojars.org/repo/" "pandect" "pandect") 
      => "http://clojars.org/repo/pandect/pandect/maven-metadata.xml"
  (build-metadata-url "http://clojars.org/repo" "org.clojure" "data.codec") 
      => "http://clojars.org/repo/org/clojure/data.codec/maven-metadata.xml")

(let [xml-string "<?xml version=\"1.0\" encoding=\"UTF-8\"?>
                  <metadata>
                    <groupId>pandect</groupId>
                    <artifactId>pandect</artifactId>
                    <versioning>
                      <release>0.2.3</release>
                      <versions>
                        <version>0.1.0-SNAPSHOT</version>
                        <version>0.1.0</version>
                        <version>0.1.1-SNAPSHOT</version>
                        <version>0.2.0</version>
                        <version>0.2.1</version>
                        <version>0.2.2</version>
                        <version>0.2.3</version>
                        <version>0.2.4-alpha</version>
                        <version>0.2.4-SNAPSHOT</version>
                      </versions>
                      <lastUpdated>20130524133042</lastUpdated>
                    </versioning>
                  </metadata>"]
  (fact "about XML parsing"
    (version-seq xml-string) => (contains ["0.1.0-SNAPSHOT" "0.1.0" "0.1.1-SNAPSHOT" 
                                           "0.2.0" "0.2.1" "0.2.2" "0.2.3"
                                           "0.2.4-alpha" "0.2.4-SNAPSHOT"]))
  (tabular
    (fact "about latest version detection."
      (:version-str (latest-version xml-string ?settings)) => ?v)
    ?settings                                      ?v
    nil                                            "0.2.3"
    {:allow-qualified true}                        "0.2.4-alpha"
    {:allow-snapshots true}                        "0.2.4-SNAPSHOT"
    {:allow-qualified true  :allow-snapshots true} "0.2.4-SNAPSHOT"))

(tabular 
  (fact "about retriever function generation"
    (metadata-retriever {:url ?url}) => ?r)
  ?url                                               ?r
  "http://repo1.maven.org/repo"                      fn?
  "https://clojars.org/repo"                         fn?
  "s3p://private/repo"                               fn?
  "file:/local/repo"                                 fn?
  "ftp://ftp/repo"                                   falsey)
