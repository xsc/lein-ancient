(defproject ancient-clj "0.1.6"
  :description "Maven Version Utilities for Clojure"
  :url "https://github.com/xsc/ancient-clj"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/data.xml "0.0.7"]
                 [colorize "0.1.1"]
                 [version-clj "0.1.0"]
                 [clj-aws-s3 "0.3.7"]
                 [clj-time "0.6.0"]
                 [clj-http "0.7.8"]
                 [commons-codec "1.8"]
                 [commons-logging "1.1.3"]]
  :repositories  {"sonatype-oss-public" "https://oss.sonatype.org/content/groups/public/"}
  :profiles {:dev {:dependencies [[midje "1.6.0" :exclusions [clj-time commons-codec]]]
                   :plugins [[lein-midje "3.1.3"]]
                   :exclusions [org.clojure/clojure]}
             :1.4 {:dependencies [[org.clojure/clojure "1.4.0"]]}
             :1.5 {:dependencies [[org.clojure/clojure "1.5.1"]]}
             :1.6 {:dependencies [[org.clojure/clojure "1.6.0-master-SNAPSHOT"]]}}
  :aliases {"all" ["with-profile" "dev,1.4:dev,1.5:dev,1.6"]}
  :pedantic? :abort)
