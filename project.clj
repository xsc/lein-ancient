(defproject ancient-clj "0.2.1-SNAPSHOT"
  :description "Maven Version Utilities for Clojure"
  :url "https://github.com/xsc/ancient-clj"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/data.xml "0.0.8"]
                 [org.clojure/core.memoize "0.5.6"]
                 [org.clojure/core.cache "0.6.4"]
                 [org.clojure/data.priority-map "0.0.5"]
                 [version-clj "0.1.0"]
                 [clj-aws-s3 "0.3.10"]
                 [clj-time "0.8.0"]
                 [http-kit "2.1.19"]
                 [joda-time "2.5"]
                 [commons-codec "1.9"]
                 [commons-logging "1.2"]
                 [potemkin "0.3.11"]]
  :exclusions [org.clojure/clojure joda-time]
  :profiles {:dev {:dependencies [[midje "1.6.3" :exclusions [clj-time commons-codec]]]
                   :plugins [[lein-midje "3.1.3"]]}
             :1.5 {:dependencies [[org.clojure/clojure "1.5.1"]]}}
  :aliases {"all"  ["with-profile" "dev:+1.5"]
            "test" ["midje"]}
  :pedantic? :abort)
