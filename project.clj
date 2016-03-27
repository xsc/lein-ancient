(defproject lein-ancient "0.6.9"
  :description "Check your Projects for outdated Dependencies."
  :url "https://github.com/xsc/lein-ancient"
  :dependencies [[org.clojure/clojure "1.8.0" :scope "provided"]
                 [ancient-clj "0.3.12"
                  :exclusions [com.amazonaws/aws-java-sdk-s3]]
                 [rewrite-clj "0.4.12"]
                 [com.amazonaws/aws-java-sdk-s3 "1.9.0"
                  :exclusions [joda-time]
                  :upgrade? false]
                 [version-clj "0.1.2"]
                 [jansi-clj "0.1.0"]
                 [org.clojure/tools.reader "0.10.0"]
                 [potemkin "0.4.3"]]
  :license {:name "MIT License"
            :url "https://opensource.org/licenses/MIT"
            :year 2013
            :key "mit"}
  :profiles {:dev {:dependencies [[midje "1.7.0" :upgrade? false]
                                  [slingshot "0.12.2"]]
                   :plugins [[lein-midje "3.1.1"]]
                   :test-paths ["test"]}}
  :aliases {"test" ["midje"]}
  :eval-in-leiningen true
  :min-lein-version "2.4.0"
  :pedantic? :abort)
