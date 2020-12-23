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
                               :exclusions [clj-http
                                            org.clojure/data.xml]]

                 ;; do not inline these
                 [clj-http "3.11.0"
                  :exclusions [commons-codec
                               commons-io]]

                 ;; pin dependencies to match Leiningen
                 [commons-codec "1.11"
                  :scope "provided"]
                 [commons-io "2.8.0"
                  :scope "provided"]
                 [org.apache.httpcomponents/httpclient "4.5.13"
                  :scope "provided"]
                 [org.clojure/data.xml "0.2.0-alpha5"
                  :exclusions [org.clojure/clojure]
                  :scope "provided"]]
  :scm {:dir ".."}

  :plugins [[lein-isolate "0.1.2"]]
  :middleware [leiningen.isolate/middleware]

  :eval-in :leiningen
  :min-lein-version "2.4.0"
  :pedantic? :abort)
