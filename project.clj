(defproject lein-ancient "0.4.3-SNAPSHOT"
  :description "Check your Projects for outdated Dependencies."
  :url "https://github.com/xsc/lein-ancient"
  :dependencies [[org.clojure/data.xml "0.0.7"]
                 [colorize "0.1.1"]
                 [clj-aws-s3 "0.3.6"]
                 [rewrite-clj "0.2.0-SNAPSHOT"]
                 [version-clj "0.1.0"]]
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :profiles {:midje {:dependencies [[midje "1.5.1"]]
                     :plugins [[lein-midje "3.1.1"]]
                     :test-paths ["test"]}}
  :aliases {"midje-dev" ["with-profile" "midje" "midje"]
            "deps-dev" ["with-profile" "midje" "deps"]}
  :eval-in-leiningen true)
