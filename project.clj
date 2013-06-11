(defproject lein-ancient "0.2.0-SNAPSHOT"
  :description "Check your Projects for outdated Dependencies."
  :url "https://github.com/xsc/lein-ancient"
  :dependencies [[org.clojure/data.xml "0.0.7"]
                 [org.clojure/tools.cli "0.2.2"]]
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :profiles {:midje {:dependencies [[midje "1.5.1"]]
                     :plugins [[lein-midje "3.0.1"]]
                     :test-paths ["test"]}}
  :aliases {"midje-dev" ["with-profile" "midje" "midje"]}
  :eval-in-leiningen true)
