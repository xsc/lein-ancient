(ns ^{ :doc "CLI processing for lein-ancient."
       :author "Yannick Scherer" }
  leiningen.ancient.cli)

(def ^:private CLI_FLAGS
  "Available CLI Flags."
  #{":dependencies" ":all" ":plugins" ":allow-snapshots"
    ":allow-qualified" ":no-profiles" ":check-clojure"
    ":verbose" ":no-colors" ":aggressive" ":print"
    ":interactive"})

(defn parse-cli
  "Parse Command Line, return map of Settings."
  [args]
  (let [data (->> (for [^String flag args]
                    (when (contains? CLI_FLAGS flag)
                      (vector
                        (keyword (.substring flag 1))
                        true)))
               (into {}))]
    (cond (:all data) (assoc data :dependencies true :plugins true) 
          (:plugins data) data
          :else (assoc data :dependencies true))))

