(ns ^{ :doc "Test for Repositories"
       :author "Yannick Scherer" }
  ancient-clj.repository-test
  (:require [midje.sweet :refer :all]
            [ancient-clj.repository :as r]
            [ancient-clj.repository.core :as rc]
            [aws.sdk.s3 :as s3 :only [get-object]]))

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

(tabular
  (tabular
    (fact "about simple repositories (using 'slurp')"
      (against-background [(slurp (rc/build-metadata-url ?url "pandect" "pandect" ?file)) => METADATA])
      (every? #(satisfies? rc/Repository %) ?repos) => truthy
      (r/retrieve-metadata-xml! ?repos "pandect" "pandect") => METADATA
      (r/retrieve-versions!     ?repos "pandect" "pandect") => (just VERSIONS)
      (r/retrieve-all-versions! ?repos "pandect" "pandect") => (just VERSIONS))
    ?repos [?url] [(r/repository ?url)] [(r/repository {:url ?url})])
  ?url            ?file
  "http://repo"   "maven-metadata.xml"
  "https://repo"  "maven-metadata.xml"
  "file://repo"   "maven-metadata-local.xml"
  "file:/repo"    "maven-metadata-local.xml")

(fact "about Amazon S3 repositories"
  (against-background [(s3/get-object 
                         {:access-key "test" :secret-key "test"} 
                         "bucket" "repo/pandect/pandect/maven-metadata.xml") 
                       => {:content (.getBytes METADATA)}])
  (let [r [(r/repository { :url "s3p://bucket/repo" :username "test" :passphrase "test"})]]
    (r/retrieve-metadata-xml! r "pandect" "pandect") => METADATA
    (r/retrieve-versions!     r "pandect" "pandect") => (just VERSIONS)
    (r/retrieve-all-versions! r "pandect" "pandect") => (just VERSIONS)))
