(defproject ancient-clj "0.7.0-SNAPSHOT"
  :description "Maven Version Utilities for Clojure"
  :url "https://github.com/xsc/ancient-clj"
  :dependencies [[org.clojure/clojure "1.10.1" :scope "provided"]
                 [org.clojure/data.xml "0.2.0-alpha6"]
                 [version-clj "0.1.2"]
                 ;; Note that this is the samve version used by s3-wagon-private 1.3.0
                 [com.amazonaws/aws-java-sdk-s3 "1.11.28"]
                 [clj-http "3.11.0"
                  :exclusions [com.cognitect/transit-clj
                               crouton
                               org.apache.httpcomponents/httpclient]]
                 [commons-logging "1.2"]
                 [joda-time "2.10.8"]
                 [potemkin "0.4.5"]]
  :scm {:dir ".."}
  :profiles {:dev {:dependencies [[midje "1.9.9"]
                                  [clj-time "0.15.2"]
                                  [http-kit "2.5.0"]]
                   :plugins [[lein-midje "3.2.2"]]}}
  :aliases {"test" ["midje"]}
  :pedantic? :abort)
