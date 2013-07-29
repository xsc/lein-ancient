(ns ^{ :doc "CLI processing for lein-ancient."
       :author "Yannick Scherer" }
  leiningen.ancient.cli)

(def ^:private CLI_FLAGS
  "Available CLI Flags."
  {":dependencies"    :dependencies
   ":all"             :all
   ":plugins"         :plugins 
   ":allow-snapshots" :snapshots?
   ":allow-qualified" :qualified?
   ":no-profiles"     :no-profiles
   ":check-clojure"   :check-clojure
   ":verbose"         :verbose
   ":no-colors"       :no-colors
   ":aggressive"      :aggressive?
   ":print"           :print
   ":interactive"     :interactive})

(defn parse-cli
  "Parse Command Line, return map of Settings."
  [args]
  (let [data (->> (for [^String flag args]
                    (when (contains? CLI_FLAGS flag)
                      (vector
                        (get CLI_FLAGS flag)
                        true)))
               (into {}))
        data (assoc data 
                    :snapshots? (:snapshots? data false)
                    :qualified? (:qualified? data false))]
    (cond (:all data) (assoc data :dependencies true :plugins true) 
          (:plugins data) data
          :else (assoc data :dependencies true))))
