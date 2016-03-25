(defproject ancient-clj "0.3.12"
  :description "Maven Version Utilities for Clojure"
  :url "https://github.com/xsc/ancient-clj"
  :license {:name "MIT License"
            :url "http://opensource.org/licenses/MIT"
            :year 2013
            :key "mit"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/tools.reader "0.10.0"]
                 [org.clojure/data.xml "0.0.8"]
                 [version-clj "0.1.2"]
                 [clj-aws-s3 "0.3.10" :exclusions [com.amazonaws/aws-java-sdk]]
                 [com.amazonaws/aws-java-sdk-s3 "1.10.64"]
                 [com.fasterxml.jackson.core/jackson-core "2.7.3"]
                 [clj-time "0.11.0"]
                 [clj-http "2.1.0"
                  :exclusions [com.cognitect/transit-clj
                               crouton
                               slingshot]]
                 [joda-time "2.9.2"]
                 [commons-codec "1.10"]
                 [commons-logging "1.2"]
                 [potemkin "0.4.3"]]
  :exclusions [org.clojure/clojure joda-time]
  :profiles {:dev {:dependencies [[midje "1.8.3" :exclusions [clj-time commons-codec]]
                                  [http-kit "2.1.19"]]
                   :plugins [[lein-midje "3.1.3"]]}
             :1.5 {:dependencies [[org.clojure/clojure "1.5.1"]]}}
  :aliases {"all"  ["with-profile" "dev:+1.5"]
            "test" ["midje"]}
  :pedantic? :abort)
