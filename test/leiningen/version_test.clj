(ns leiningen.version-test
  (:use midje.sweet
        leiningen.ancient.version))

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
