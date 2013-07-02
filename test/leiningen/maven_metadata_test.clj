(ns leiningen.maven-metadata-test
  (:use midje.sweet
        leiningen.ancient.maven-metadata
        [leiningen.ancient.maven-metadata.utils :only [build-metadata-url]]))

(fact "about metadata XML"
  (build-metadata-url "http://clojars.org/repo" "pandect" "pandect") 
      => "http://clojars.org/repo/pandect/pandect/maven-metadata.xml"
  (build-metadata-url "http://clojars.org/repo" "pandect" "pandect" "maven-metadata-local.xml") 
      => "http://clojars.org/repo/pandect/pandect/maven-metadata-local.xml"
  (build-metadata-url "http://clojars.org/repo/" "pandect" "pandect") 
      => "http://clojars.org/repo/pandect/pandect/maven-metadata.xml"
  (build-metadata-url "http://clojars.org/repo" "org.clojure" "data.codec") 
      => "http://clojars.org/repo/org/clojure/data.codec/maven-metadata.xml"
) 
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
