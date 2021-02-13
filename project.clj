(defproject lein-ancient "1.0.0-RC1"
  :description "Check your Projects for outdated Dependencies."
  :url "https://github.com/xsc/lein-ancient"
  :license {:name "MIT"
            :comment "MIT License"
            :url "https://choosealicense.com/licenses/mit"
            :year 2013
            :key "mit"}
  :dependencies [[org.clojure/clojure "1.10.2" :scope "provided"]

                 ;; often-used dependencies to include as source
                 ;; (to avoid dependency conflicts)
                 ^:inline-dep [rewrite-clj "0.6.1"]
                 ^:inline-dep [org.clojure/tools.reader "1.3.5"]
                 ^:inline-dep [potemkin "0.4.5"]
                 ^:inline-dep [version-clj "1.0.0"]
                 ^:inline-dep [jansi-clj "0.1.1"]
                 ^:inline-dep [ancient-clj "1.0.0"
                               :exclusions [clj-commons/pomegranate
                                            potemkin
                                            riddley]]

                 ;; do not inline these
                 [clj-commons/pomegranate "1.2.0"
                  :scope "provided"
                  :exclusions [org.apache.httpcomponents/httpclient
                               org.apache.httpcomponents/httpcore
                               org.clojure/clojure]
                  :upgrade false]

                 ;; pin dependencies to match Leiningen
                 [commons-codec "1.11"
                  :scope "provided"
                  :upgrade false]
                 [commons-io "2.8.0"
                  :scope "provided"
                  :upgrade false]
                 [org.apache.httpcomponents/httpclient "4.5.13"
                  :scope "provided"
                  :upgrade false]
                 [org.clojure/data.xml "0.2.0-alpha5"
                  :exclusions [org.clojure/clojure]
                  :scope "provided"
                  :upgrade false]]

  :plugins [[lein-isolate "0.2.1"]]
  :middleware [leiningen.isolate/middleware]

  :eval-in :leiningen
  :min-lein-version "2.4.0"
  :pedantic? :abort)
