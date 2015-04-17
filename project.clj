(defproject lein-ancient "0.6.7-SNAPSHOT"
  :description "Check your Projects for outdated Dependencies."
  :url "https://github.com/xsc/lein-ancient"
  :dependencies [[rewrite-clj "0.4.12"]
                 [ancient-clj "0.3.9"
                  :exclusions [com.amazonaws/aws-java-sdk-s3]]
                 [com.amazonaws/aws-java-sdk-s3 "1.9.0"
                  :exclusions [joda-time]]
                 [version-clj "0.1.2"]
                 [jansi-clj "0.1.0"]
                 [org.clojure/tools.reader "0.9.1"]
                 [potemkin "0.3.13"]
                 [joda-time "2.7"]
                 [commons-io "2.4"]]
  :license {:name "MIT License"
            :url "http://xsc.mit-license.org"}
  :profiles {:dev {:dependencies [[midje "1.6.3"]
                                  [slingshot "0.12.2"]]
                   :plugins [[lein-midje "3.1.1"]]
                   :test-paths ["test"]}}
  :aliases {"test" ["midje"]}
  :eval-in-leiningen true
  :min-lein-version "2.4.0"
  :pedantic? :abort)
