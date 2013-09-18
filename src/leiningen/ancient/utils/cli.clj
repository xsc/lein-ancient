(ns ^{ :doc "CLI processing for lein-ancient."
       :author "Yannick Scherer" }
  leiningen.ancient.utils.cli)

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
   ":interactive"     :interactive
   ":no-tests"        :no-tests
   ":overwrite-backup":overwrite-backup})

(def ^:private CLI_DEPRECATED
  "Flags that are no longer supported."
  {":upgrade"        "use task 'upgrade' instead"
   ":upgrade-global" "use task 'upgrade-global' instead"
   ":get"            "use task 'get' instead"})

(defn parse-cli
  "Parse Command Line, return map of Settings."
  [args]
  (let [data (->> (for [^String flag args]
                    (cond (contains? CLI_DEPRECATED flag) (println "WARN: option" (str "'" flag "'") "is no longer supported"
                                                                   (str "(" (get CLI_DEPRECATED flag) ")."))
                          (contains? CLI_FLAGS flag) (vector
                                                       (get CLI_FLAGS flag)
                                                       true)
                          :else (println "WARN: option" (str "'" flag "'") "not recognized.")))
               (into {}))
        data (assoc data 
                    :aggressive? (:aggressive? data false)
                    :snapshots?  (:snapshots? data false)
                    :qualified?  (:qualified? data false))]
    (cond (:all data) (assoc data :dependencies true :plugins true) 
          (:plugins data) data
          :else (assoc data :dependencies true))))
