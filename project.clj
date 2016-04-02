(defproject ancient-clj "0.3.14"
  :description "Maven Version Utilities for Clojure"
  :url "https://github.com/xsc/ancient-clj"
  :license {:name "MIT License"
            :url "https://opensource.org/licenses/MIT"
            :year 2013
            :key "mit"}
  :dependencies [[org.clojure/clojure "1.8.0" :scope "provided"]
                 [org.clojure/data.xml "0.0.8"]
                 [version-clj "0.1.2"]
                 [clj-aws-s3 "0.3.10" :exclusions [com.amazonaws/aws-java-sdk]]
                 [com.amazonaws/aws-java-sdk-s3 "1.10.66"]
                 [clj-http "2.1.0"
                  :exclusions [com.cognitect/transit-clj
                               crouton
                               slingshot]]
                 [commons-logging "1.2"]
                 [joda-time "2.9.2"]
                 [potemkin "0.4.3"]]
  :profiles {:dev {:dependencies [[midje "1.8.3"]
                                  [clj-time "0.11.0"]
                                  [http-kit "2.1.19"]]
                   :plugins [[lein-midje "3.1.3"]]}}
  :aliases {"test" ["midje"]}
  :pedantic? :abort)
