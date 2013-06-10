(ns leiningen.ancient-test
  (:use midje.sweet
        leiningen.ancient))

(tabular
  (fact "about version map creation"
    (let [m (version-map ?version)]
      (:major m) => ?major
      (:minor m) => ?minor
      (:incremental m) => ?incr
      (:qualifier m) => ?q
      (:version-str m) => ?version))
  ?version         ?major ?minor ?incr ?q
  "1.0.0"          1      0      0     nil
  "1.0"            1      0      0     nil
  "1"             -1     -1     -1     "1"
  "1.0.1-SNAPSHOT" 1      0      1     "snapshot"
  "1.0.1-alpha2"   1      0      1     "alpha2")

(fact "about SNAPSHOTs"
  (version-map "1.0.0") =not=> snapshot?
  (version-map "1.0.0-SNAPSHOT") => snapshot?)

(tabular
  (fact "about version comparison"
    (version-compare ?v0 ?v1) => ?r)
  ?v0              ?v1               ?r
  ;; Numeric Comparison
  "1.0.0"          "1.0.0"           0
  "1.0.0"          "1.0"             0
  "1.0.0"          "1.0.1"          -1
  "1.0.0"          "0.9.2"           1
  "0.9.2"          "0.9.3"          -1
  "0.9.2"          "0.9.1"           1
  "0.9.5"          "0.9.13"         -1
  "1.0.0-SNAPSHOT" "1.0.1-SNAPSHOT" -1
  "1.0.0-alpha"    "1.0.1-beta"     -1

  ;; Lexical Comparison
  "1.0.0-alpha"    "1.0.0-beta"     -1
  "1.0.0-beta"     "1.0.0-alpha"     1

  ;; Lexical/Numeric Comparison
  "1.0.0-alpha1"   "1.0.0-alpha2"   -1
  "1.0.0-alpha5"   "1.0.0-alpha23"  -1

  ;; Releases are newer than SNAPSHOTs
  "1.0.0"          "1.0.0-SNAPSHOT"  1
  "1.0.0-SNAPSHOT" "1.0.0-SNAPSHOT"  0
  "1.0.0-SNAPSHOT" "1.0.0"          -1

  ;; Releases are newer than qualified versions
  "1.0.0"          "1.0.0-alpha5"    1
  "1.0.0-alpha5"   "1.0.0"          -1

  ;; SNAPSHOTS are newer than qualified versions
  "1.0.0-SNAPSHOT" "1.0.0-RC1"       1
  "1.0.0-SNAPSHOT" "1.0.1-RC1"      -1)

(fact "about metadata XML"
  (build-metadata-url "http://clojars.org/repo" "pandect" "pandect") 
      => "http://clojars.org/repo/pandect/pandect/maven-metadata.xml"
  (build-metadata-url "http://clojars.org/repo/" "pandect" "pandect") 
      => "http://clojars.org/repo/pandect/pandect/maven-metadata.xml"
  (build-metadata-url "http://clojars.org/repo" "org.clojure" "data.codec") 
      => "http://clojars.org/repo/org/clojure/data.codec/maven-metadata.xml"
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
                        </versions>
                        <lastUpdated>20130524133042</lastUpdated>
                      </versioning>
                    </metadata>"]
    (version-seq xml-string) => (contains ["0.1.0-SNAPSHOT" "0.1.0" "0.1.1-SNAPSHOT" "0.2.0" "0.2.1" "0.2.2" "0.2.3"])
    (:version-str (latest-version xml-string)) => "0.2.3"))
