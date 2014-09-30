(defproject ancient-clj "0.2.0-SNAPSHOT"
  :description "Maven Version Utilities for Clojure"
  :url "https://github.com/xsc/ancient-clj"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/data.xml "0.0.8"]
                 [version-clj "0.1.0"]
                 [clj-aws-s3 "0.3.10"]
                 [clj-time "0.8.0"]
                 [clj-http "1.0.0"]
                 [com.fasterxml.jackson.core/jackson-core "2.4.2"]
                 [com.fasterxml.jackson.core/jackson-annotations "2.4.2"]
                 [com.fasterxml.jackson.core/jackson-databind "2.4.2"]
                 [joda-time "2.4"]
                 [commons-codec "1.9"]
                 [commons-logging "1.2"]]
  :exclusions [org.clojure/clojure joda-time]
  :repositories  {"sonatype-oss-public" "https://oss.sonatype.org/content/groups/public/"}
  :profiles {:dev {:dependencies [[midje "1.6.3" :exclusions [clj-time commons-codec]]]
                   :plugins [[lein-midje "3.1.3"]]}
             :1.5 {:dependencies [[org.clojure/clojure "1.5.1"]]}}
  :aliases {"all"  ["with-profile" "dev:+1.5"]
            "test" ["midje"]}
  :pedantic? :abort)
