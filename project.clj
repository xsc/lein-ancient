(defproject lein-ancient-parent "0.7.0-SNAPSHOT"
  :description "Check your Projects for outdated Dependencies"
  :url "https://github.com/xsc/lein-ancient"
  :license {:name "MIT License"
            :url "https://opensource.org/licenses/MIT"
            :year 2013
            :key "mit"}

  :plugins [[lein-monolith "1.0.1"]
            [org.clojure/core.rrb-vector "0.1.0"
             :exclusions [org.clojure/clojure]]]

  :monolith
  {:project-dirs ["ancient-clj" "lein-ancient"]})
