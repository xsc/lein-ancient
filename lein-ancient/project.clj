(defproject lein-ancient "0.7.0-SNAPSHOT"
  :description "Check your Projects for outdated Dependencies."
  :url "https://github.com/xsc/lein-ancient"
  :dependencies [[org.clojure/clojure "1.10.1" :scope "provided"]

                 ;; often-used dependencies to include as source
                 ;; (to avoid dependency conflicts)
                 ^:source-dep [rewrite-clj "0.6.1"]
                 ^:source-dep [org.clojure/tools.reader "1.3.4"]
                 ^:source-dep [potemkin "0.4.5"]
                 ^:source-dep [version-clj "0.1.2"]
                 ^:source-dep [jansi-clj "0.1.1"]
                 ^:source-dep [ancient-clj "0.7.0-SNAPSHOT"
                               :exclusions [com.amazonaws/aws-java-sdk-s3
                                            clj-http]]

                 [clj-http "3.11.0"
                  :exclusions [commons-codec
                               org.apache.httpcomponents/httpcore]]

                 ;; S3 dependency is pinned because of conflicts of newer
                 ;; versions with Leiningen's precompiled dependencies.
                 [com.amazonaws/aws-java-sdk-s3 "1.11.28"
                  :exclusions [joda-time commons-logging]
                  :upgrade? false]]
  :license {:name "MIT License"
            :url "https://opensource.org/licenses/MIT"
            :year 2013
            :key "mit"}
  :scm {:dir ".."}
  :profiles {:dev {:dependencies [[midje "1.9.9"]
                                  [slingshot "0.12.2"]
                                  [fipp "0.6.23"]]
                   :plugins [[lein-midje "3.2.2"]]
                   :test-paths ["test"]}}
  :aliases {"test" ["midje"]}

  :plugins [[lein-isolate "0.1.1"]]
  :middleware [leiningen.isolate/middleware]

  :eval-in :leiningen
  :min-lein-version "2.4.0"
  :pedantic? :abort)
