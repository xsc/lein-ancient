(defproject lein-ancient "0.6.0-SNAPSHOT"
  :description "Check your Projects for outdated Dependencies."
  :url "https://github.com/xsc/lein-ancient"
  :dependencies [[rewrite-clj "0.4.4"]
                 [ancient-clj "0.3.1"]
                 [jansi-clj "0.1.0"]
                 [potemkin "0.3.11"]
                 [commons-io "2.4"]]
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :profiles {:dev {:dependencies [[midje "1.6.3"]]
                   :plugins [[lein-midje "3.1.1"]]
                   :test-paths ["test"]}}
  :aliases {"test" ["midje"]}
  :eval-in-leiningen true
  :pedantic? :abort)
