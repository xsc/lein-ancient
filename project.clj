(defproject ancient-clj "0.1.5"
  :description "Maven Version Utilities for Clojure"
  :url "https://github.com/xsc/ancient-clj"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/data.xml "0.0.7"]
                 [colorize "0.1.1" :exclusions [org.clojure/clojure]]
                 [version-clj "0.1.0" :exclusions [org.clojure/clojure]]
                 [clj-aws-s3 "0.3.7" :exclusions [org.clojure/clojure org.apache.httpcomponents/httpclient commons-codec]]
                 [org.apache.httpcomponents/httpclient "4.3.1"]
                 [commons-codec "1.8"]]
  :repositories  {"sonatype-oss-public" "https://oss.sonatype.org/content/groups/public/"}
  :profiles {:dev {:dependencies [[midje "1.5.1"]
                                  [org.codehaus.plexus/plexus-utils "3.0.15"]]
                   :plugins [[lein-midje "3.1.1"]]}
             :1.4 {:dependencies [[org.clojure/clojure "1.4.0"]]}
             :1.5 {:dependencies [[org.clojure/clojure "1.5.1"]]}
             :1.6 {:dependencies [[org.clojure/clojure "1.6.0-master-SNAPSHOT"]]}}
  :aliases {"midje-dev" ["with-profile" "dev,1.4:dev,1.5:dev,1.6" "midje"]
            "deps-dev" ["with-profile" "dev,1.4:dev,1.5:dev,1.6" "deps"]}
  :pedantic? :abort)
