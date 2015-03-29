(defproject ancient-clj "0.3.7-SNAPSHOT"
  :description "Maven Version Utilities for Clojure"
  :url "https://github.com/xsc/ancient-clj"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/data.xml "0.0.8"]
                 [version-clj "0.1.2"]
                 [clj-aws-s3 "0.3.10" :exclusions [com.amazonaws/aws-java-sdk]]
                 [com.amazonaws/aws-java-sdk-s3 "1.9.27"]
                 [com.fasterxml.jackson.core/jackson-core "2.5.1"]
                 [clj-time "0.9.0"]
                 [clj-http "1.1.0" :exclusions [com.cognitect/transit-clj]]
                 [joda-time "2.7"]
                 [commons-codec "1.10"]
                 [commons-logging "1.2"]
                 [potemkin "0.3.12"]]
  :exclusions [org.clojure/clojure joda-time]
  :profiles {:dev {:dependencies [[midje "1.6.3" :exclusions [clj-time commons-codec]]
                                  [http-kit "2.1.19"]]
                   :plugins [[lein-midje "3.1.3"]]}
             :1.5 {:dependencies [[org.clojure/clojure "1.5.1"]]}}
  :aliases {"all"  ["with-profile" "dev:+1.5"]
            "test" ["midje"]}
  :pedantic? :abort)
