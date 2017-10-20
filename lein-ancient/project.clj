(defproject lein-ancient "0.6.15-SNAPSHOT"
  :description "Check your Projects for outdated Dependencies."
  :url "https://github.com/xsc/lein-ancient"
  :dependencies [[org.clojure/clojure "1.8.0" :scope "provided"]

                 ;; often-used dependencies to include as source
                 ;; (to avoid dependency conflicts)
                 ^:source-dep [rewrite-clj "0.4.13"]
                 ^:source-dep [org.clojure/tools.reader "0.10.0"]
                 ^:source-dep [potemkin "0.4.4"]
                 ^:source-dep [version-clj "0.1.2"]
                 ^:source-dep [jansi-clj "0.1.0"]
                 ^:source-dep [ancient-clj "0.6.14"
                               :exclusions [com.amazonaws/aws-java-sdk-s3
                                            clj-http]]

                 [clj-http "3.7.0"
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
  :profiles {:dev {:dependencies [[midje "1.8.3"]
                                  [slingshot "0.12.2"]]
                   :plugins [[lein-midje "3.1.1"]]
                   :test-paths ["test"]}}
  :plugins [[thomasa/mranderson "0.4.7"]]
  :aliases {"test"     ["midje"]
            "build"    ["do" "clean," "source-deps"
                        ":skip-javaclass-repackage" "true"
                        ":project-prefix" "ancient"]
            "isolated" ["do" "build," "with-profile" "+plugin.mranderson/config"]}
  :eval-in-leiningen true
  :min-lein-version "2.4.0"
  :pedantic? :abort)
