(ns leiningen.ancient.cli
  (:require [leiningen.ancient.verbose :refer :all]
            [clojure.string :as string]))

;; ## Flags

(def ^:private flags
  "All available flags and effects."
  {:all             [#{:dependencies? :plugins? :java-agents?}
                     "check dependencies, plugins, _and_ java-agents."]
   :allow-all       [#{:snapshots? :qualified?}
                     "allow SNAPSHOT and qualified versions to be reported as new."]
   :allow-snapshots [#{:snapshots?}
                     "allow SNAPSHOT versions to be reported as new."]
   :allow-qualified [#{:qualified?}
                     "allow alpha, beta, etc... versions to be reported as new."]
   :check-clojure   [#{:check-clojure?}
                     "include Clojure (org.clojure/clojure) when checking for outdated artifacts."]
   :interactive     [#{:interactive?}
                     "prompt the user for approval of any changes made during upgrade."]
   :plugins         [#{:no-dependencies? :no-java-agents? :plugins?}
                     "check _only_ plugins."]
   :java-agents     [#{:no-dependencies? :no-plugins? :java-agents?}
                     "check _only_ java-agents"]
   :no-colors       [#{:no-colors?}
                     "explicitly disable colorized output."]
   :no-colours      [#{:no-colors?}
                     "explicitly disable colorized output."]
   :no-profiles     [#{:no-profiles?}
                     "exclude non-user profiles from processing."]
   :no-tests        [#{:no-tests?}
                     "disable tests."]
   :tests           [#{:tests?}
                     "force tests."]
   :print           [#{:print? :no-tests?}
                     "print processing result to stdout."]
   :recursive       [#{:recursive?}
                     "recursively process files at the given or current paths."]})

(defn- split-keywords-into
  [k]
  (fn [opts ^String v]
    (->> (string/split v #",")
         (map keyword)
         (assoc opts k))))

(def ^:private settings
  "All available settings and their parsers."
  {:only [(split-keywords-into :only)
          "low-level setting to determine which artifact types to process."]
   :exclude [(split-keywords-into :exclude)
             "low-level setting to determine which artifact types to ignore."]})

(defn- keys->strings
  [m]
  (->> (for [[k v] m]
         [(str k) v])
       (into {})))

(def ^:private flag-order
  "Flag processing order (the higher, the earlier a flag will be processed)."
  (->> {:no-colors  5
        :no-colours 5
        :plugins    4
        :print      4}
       (keys->strings)))

(defn- ->effect
  "Create option key and effect from a single keyword optionally
   prefixed with `:no-`."
  [v]
  (let [s (name v)
        [k' t] (if (.startsWith s "no-")
                 [(subs s 3) false]
                 [s true])
        k (keyword k')]
    [k t]))

(defn- effect-fn
  "Create a function that will update an option map
   based on whether or not the given effect keyword starts
   with 'no-' or not."
  [v]
  (let [[k t] (->effect v)]
    #(assoc % k t)))

(def ^:private flag-fns
  "Map of flag strings and effect functions."
  (->> (for [[k vs'] flags
             :let [vs (first vs')]]
         (->> (map effect-fn vs)
              (apply comp)
              (vector (str k))))
       (into {})))

(def ^:private setting-fns
  "Map of setting strings to effect functions."
  (->> (for [[k [f _]] settings]
         [(str k) f])
       (into {})))

(def ^:private defaults
  "Default settings."
  (-> (reduce
        (fn [acc vs]
          (->> (map (comp first ->effect) vs)
               (apply conj acc)))
        #{}
        (map first (vals flags)))
      (zipmap (repeat false))
      (merge
        {:only          []
         :exclude       []
         :dependencies? true
         :profiles?     true
         :java-agents?  true
         :colors?       true
         :tests?        true})))

;; ## Deprecated/Supported

(def ^:private deprecated
  "Flags that are no longer supported."
  (->> {:aggressive       "all lookups are now aggressive"
        :overwrite-backup "a backup file is no longer needed"
        :upgrade          "use task 'upgrade' instead"
        :upgrade-global   "use task 'upgrade-global' instead"
        :get              "use task 'get' instead"
        :dependencies     "since it is the default behaviour"
        :verbose          "run 'DEBUG=1 lein ancient ...' instead"}
       (keys->strings)))

(defn- unrecognized!
  "Print error message and throw Exception."
  [arg]
  (let [msg (format "option '%s' not recognized." arg)]
    (throw (ex-info msg {:arg arg}))))

(defn- applicable?
  [arg exclude?]
  (if (exclude? arg)
    (warnf "option '%s' is not applicable for this task." arg)
    (if-let [msg (deprecated arg)]
      (warnf "option '%s' is no longer supported. (%s)" arg msg)
      true)))

(defn- supported-flag?
  "Check if an argument is supported and print warning if not. Returns
   a flag function if argument is supported, nil otherwise."
  [arg exclude?]
  (when (applicable? arg exclude?)
    (if (contains? flag-fns arg)
      (flag-fns arg)
      (unrecognized! arg))))

(defn- supported-setting?
  "Check if an argument is supported and print warning if not. Returns
   a setting function if argument is supported, nil otherwise."
  [arg exclude?]
  (when (applicable? arg exclude?)
    (if (contains? setting-fns arg)
      (setting-fns arg)
      (unrecognized! arg))))

;; ## Parse

(defn- flag?
  "Check whether a string represents a flag."
  [s]
  (or (vector? s)
      (.startsWith ^String s ":")))

(defn- group-args
  [args]
  (loop [args args
         result []]
    (if (seq args)
      (let [[flag & rst] args]
        (if (contains? setting-fns flag)
          (recur (next rst) (conj result [flag (first rst)]))
          (recur rst (conj result flag))))
      result)))

(defn- split-args
  "Split command line arguments into flags and rest arguments."
  [args]
  (let [args (group-args args)
        [fls rst] (split-with flag? args)]
    (->> (if (= (first rst) "--")
           (rest rst)
           rst)
         (vector fls))))

(defn- read-arg
  [exclude? opts flag]
  (if (vector? flag)
    (let [[flag value] flag]
      (if-let [f (supported-setting? flag exclude?)]
        (f opts value)
        opts))
    (if-let [f (supported-flag? flag exclude?)]
      (f opts)
      opts)))

(defn parse
  "Parse the given command line args and produce a pair of
   options/remaining."
  [args & {:keys [change-defaults exclude]}]
  (let [exclude? (set (map str exclude))
        [fls rst] (split-args args)
        opts (->> fls
                  (sort-by #(flag-order % 0))
                  (reverse)
                  (reduce
                    #(read-arg exclude? %1 %2)
                    (merge defaults change-defaults)))]
    [opts rst]))

;; ## Documentation

(defn- caption
  [s]
  (->> (repeat (count s) \-)
       (apply str)
       (format "%n%n  %s%n  %s%n%n" s)))

(defn doc!
  "Attach CLI doc to var metadata."
  [task-var msg & {:keys [exclude]}]
  (->> (reduce dissoc (merge flags settings) exclude)
       (sort-by key)
       (map
         (fn [[k [_ doc]]]
           (format "    %-25s %s" (pr-str k) doc)))
       (string/join (format "%n"))
       (str
         "  " msg
         (caption "CLI Options"))
       (alter-meta! task-var assoc :doc))
  (alter-meta! task-var assoc :arglists '([project & args]))
  task-var)
