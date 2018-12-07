(defproject lein-ancient-monolith "MONOLITH"
  :dependencies [[org.clojure/clojure "1.8.0"]]
  :plugins [[lein-monolith "1.0.1"]
            ;; Add RRB vector manually until lein-monolith releases
            ;; https://github.com/amperity/lein-monolith/pull/39
            [org.clojure/core.rrb-vector "0.0.13"]]
  :monolith
  {:project-dirs ["ancient-clj" "lein-ancient"]})
