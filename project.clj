(defproject lein-ancient "0.5.5-SNAPSHOT"
  :description "Check your Projects for outdated Dependencies."
  :url "https://github.com/xsc/lein-ancient"
  :dependencies [[rewrite-clj "0.3.8"]
                 [ancient-clj "0.1.9"]
                 [jansi-clj "0.1.0-SNAPSHOT"]]
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :profiles {:dev {:dependencies [[midje "1.6.0"]]
                   :plugins [[lein-midje "3.1.1"]]
                   :test-paths ["test"]}}
  :eval-in-leiningen true
  :pedantic? :abort)
