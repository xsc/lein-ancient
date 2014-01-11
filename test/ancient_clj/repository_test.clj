(ns ^{ :doc "Test for Repositories"
       :author "Yannick Scherer" }
  ancient-clj.repository-test
  (:require [midje.sweet :refer :all]
            [ancient-clj.repository :as r]
            [ancient-clj.repository.core :as rc]
            [ancient-clj.io :as io]
            [aws.sdk.s3 :as s3 :only [get-object]]))

;; ## Fixtures

(def METADATA
  "<?xml version=\"1.0\" encoding=\"UTF-8\"?>
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
   </metadata>")

(def VERSIONS ["0.1.0-SNAPSHOT" "0.1.0" "0.1.1-SNAPSHOT" "0.2.0"
               "0.2.1" "0.2.2" "0.2.3" "0.2.4-alpha" "0.2.4-SNAPSHOT"])

(def METADATA2
  "<?xml version=\"1.0\" encoding=\"UTF-8\"?>
   <metadata>
     <groupId>pandect</groupId>
     <artifactId>pandect</artifactId>
     <versioning>
       <versions>
         <version>0.2.2</version>
         <version>0.3.0-SNAPSHOT</version>
       </versions>
       <lastUpdated>20130524133042</lastUpdated>
     </versioning>
   </metadata>")

(def VERSIONS2 ["0.3.0-SNAPSHOT"])

;; ## Tests

(fact "about metadata URL creation"
  (io/build-metadata-url "http://clojars.org/repo" "pandect" "pandect")
      => "http://clojars.org/repo/pandect/pandect/maven-metadata.xml"
  (io/build-metadata-url "http://clojars.org/repo" "pandect" "pandect" "maven-metadata-local.xml")
      => "http://clojars.org/repo/pandect/pandect/maven-metadata-local.xml"
  (io/build-metadata-url "http://clojars.org/repo/" "pandect" "pandect")
      => "http://clojars.org/repo/pandect/pandect/maven-metadata.xml"
  (io/build-metadata-url "http://clojars.org/repo" "org.clojure" "data.codec")
      => "http://clojars.org/repo/org/clojure/data.codec/maven-metadata.xml")

(tabular
  (tabular
    (fact "about simple repositories (using 'slurp')"
      (against-background [(slurp (io/build-metadata-url ?url "pandect" "pandect" "maven-metadata.xml")) => METADATA
                           (slurp (io/build-metadata-url ?url "pandect" "pandect" "maven-metadata-local.xml")) => nil])
      (every? #(satisfies? rc/Repository %) ?repos) => truthy
      (r/retrieve-metadata-xml!    ?repos "pandect" "pandect") => METADATA
      (r/retrieve-version-strings! ?repos "pandect" "pandect") => (just VERSIONS)
      (r/retrieve-latest-version-string! ?repos "pandect" "pandect") => "0.2.4-SNAPSHOT"
      (r/retrieve-latest-version-string!
        {:snapshots? false}
        ?repos "pandect" "pandect") => "0.2.4-alpha"
      (r/retrieve-latest-version-string!
        {:qualified? false}
        ?repos "pandect" "pandect") => "0.2.4-SNAPSHOT"
      (r/retrieve-latest-version-string!
        {:qualified? false :snapshots? false}
        ?repos "pandect" "pandect") => "0.2.3")
    ?repos [?url] [(r/repository ?url)] [(r/repository {:url ?url})])
  ?url "http://repo" "https://repo" "file://repo" "file:/repo")

(fact "about Amazon S3 repositories"
  (against-background [(s3/get-object
                         {:access-key "test" :secret-key "test"}
                         "bucket" "repo/pandect/pandect/maven-metadata.xml")
                       => {:content (.getBytes METADATA)}])
  (let [r [(r/repository { :url "s3p://bucket/repo" :username "test" :passphrase "test"})]]
    (r/retrieve-metadata-xml!    r "pandect" "pandect") => METADATA
    (r/retrieve-version-strings! r "pandect" "pandect") => (just VERSIONS)
    (r/retrieve-latest-version-string!
      {:snapshots? false}
      r "pandect" "pandect") => "0.2.4-alpha"
    (r/retrieve-latest-version-string!
      {:qualified? false}
      r "pandect" "pandect") => "0.2.4-SNAPSHOT"
    (r/retrieve-latest-version-string!
      {:qualified? false :snapshots? false}
      r "pandect" "pandect") => "0.2.3"))

(tabular
  (tabular
    (fact "about multiple repository analysis"
      (against-background [(slurp (io/build-metadata-url ?url1 "pandect" "pandect" "maven-metadata.xml")) => METADATA
                           (slurp (io/build-metadata-url ?url1 "pandect" "pandect" "maven-metadata-local.xml")) => nil
                           (slurp (io/build-metadata-url ?url2 "pandect" "pandect" "maven-metadata.xml")) => METADATA2
                           (slurp (io/build-metadata-url ?url2 "pandect" "pandect" "maven-metadata-local.xml")) => nil])
      (when (not= ?url1 ?url2)
        (r/retrieve-version-strings! [?url1 ?url2] "pandect" "pandect") => (just (concat VERSIONS VERSIONS2))
        (r/retrieve-latest-version-string!
          [?url1 ?url2] "pandect" "pandect") => "0.3.0-SNAPSHOT"
        (r/retrieve-latest-version-string!
          {:snapshots? false}
          [?url1 ?url2] "pandect" "pandect") => "0.2.4-alpha"
        (r/retrieve-latest-version-string!
          {:qualified? false}
          [?url1 ?url2] "pandect" "pandect") => "0.3.0-SNAPSHOT"
        (r/retrieve-latest-version-string!
          {:snapshots? false :qualified? false}
          [?url1 ?url2] "pandect" "pandect") => "0.2.3"))
    ?url2 "http://repo" "https://repo" "file://repo" "file:/repo")
  ?url1 "http://repo" "https://repo" "file://repo" "file:/repo")

(tabular
  (fact "about unknown repository types"
    (r/repository ?url) => falsey)
  ?url
  "ftp://repo"
  "git://repo")
