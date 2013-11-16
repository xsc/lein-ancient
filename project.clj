(defproject lein-ancient "0.5.4-SNAPSHOT"
  :description "Check your Projects for outdated Dependencies."
  :url "https://github.com/xsc/lein-ancient"
  :dependencies [[rewrite-clj "0.3.4"]
                 [ancient-clj "0.1.4"]]
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :profiles {:midje {:dependencies [[midje "1.5.1"]]
                     :plugins [[lein-midje "3.1.1"]]
                     :test-paths ["test"]}}
  :aliases {"midje-dev" ["with-profile" "midje" "midje"]
            "deps-dev" ["with-profile" "midje" "deps"]}
  :eval-in-leiningen true)
