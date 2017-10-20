(defproject lein-ancient-monolith "MONOLITH"
  :dependencies [[org.clojure/clojure "1.8.0"]]
  :plugins [[lein-monolith "1.0.1"]]
  :monolith
  {:project-dirs ["ancient-clj" "lein-ancient"]})
