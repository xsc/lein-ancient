(ns ^{ :doc "CLI processing for lein-ancient."
       :author "Yannick Scherer" }
  leiningen.ancient.utils.cli
  (:require [leiningen.core.main :as main]))

(def ^:private CLI_FLAGS
  "Available CLI Flags (and how they affect the settings map)."
  {":all"             [:dependencies     true
                       :plugins          true]
   ":plugins"         [:plugins          true
                       :dependencies     false]
   ":allow-snapshots" [:snapshots?       true]
   ":allow-qualified" [:qualified?       true]
   ":allow-all"       [:snapshots?       true
                       :qualified?       true]
   ":no-profiles"     [:profiles         false]
   ":check-clojure"   [:check-clojure    true]
   ":no-colors"       [:no-colors        true]
   ":aggressive"      [:aggressive?      true]
   ":print"           [:print            true]
   ":interactive"     [:interactive      true]
   ":no-tests"        [:tests            false]
   ":overwrite-backup"[:overwrite-backup true]})

(def ^:private CLI_DEFAULTS
  (merge
    (into {} (for [[k _] (mapcat #(partition 2 %) (vals CLI_FLAGS))]
               [k false]))
    {:dependencies true
     :profiles     true
     :tests        true}))

(def ^:private CLI_DEPRECATED
  "Flags that are no longer supported."
  {":upgrade"        "use task 'upgrade' instead"
   ":upgrade-global" "use task 'upgrade-global' instead"
   ":get"            "use task 'get' instead"
   ":dependencies"   "since it is the default behaviour"
   ":verbose"        "run 'DEBUG=1 lein ancient ...' instead"})

(defn- supported?
  "Check if an argument is supported and print warning if not. Returns
   true if argument is supported, nil otherwise."
  [arg]
  (if-let [msg (CLI_DEPRECATED arg)]
    (main/info "WARN: option" (str "'" arg "'") "is no longer supported"
             (str "(" msg ")."))
    (or (contains? CLI_FLAGS arg)
        (main/info "WARN: option" (str "'" arg "'") "not recognized."))))

(defn parse-cli
  "Take seq of CLI arguments and produce a map of settings."
  [args]
  (reduce
    (fn [settings arg]
      (if-not (supported? arg)
        settings
        (apply assoc settings (CLI_FLAGS arg))))
    CLI_DEFAULTS args))
