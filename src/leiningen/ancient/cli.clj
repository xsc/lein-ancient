(ns leiningen.ancient.cli
  (:require [leiningen.ancient.verbose :refer :all]
            [clojure.string :as string]))

;; ## Flags

(def ^:private flags
  "All available flags and effects."
  {:all             [#{:dependencies? :plugins?}
                     "check dependencies _and_ plugins."]
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
   :plugins         [#{:no-dependencies? :plugins?}
                     "check _only_ plugins."]
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
        {:dependencies? true
         :profiles?     true
         :colors?       true
         :tests?        true})))

;; ## Deprecated/Supported

(def ^:private deprecated
  "Flags that are no longer supported."
  (->> {:aggressive     "all lookups are now aggressive"
        :upgrade        "use task 'upgrade' instead"
        :upgrade-global "use task 'upgrade-global' instead"
        :get            "use task 'get' instead"
        :dependencies   "since it is the default behaviour"
        :verbose        "run 'DEBUG=1 lein ancient ...' instead"}
       (map
         (fn [[k v]]
           [(str k) v]))
       (into {})))

(defn- unrecognized!
  "Print error message and throw Exception."
  [arg]
  (let [msg (format "option '%s' not recognized." arg)]
    (throw (ex-info msg {:arg arg}))))

(defn- supported?
  "Check if an argument is supported and print warning if not. Returns
   true if argument is supported, nil otherwise."
  [arg exclude?]
  (if (exclude? arg)
    (warnf "option '%s' is not applicable for this task." arg)
    (if-let [msg (deprecated arg)]
      (warnf "option '%s' is no longer supported. (%s)" arg msg)
      (if (contains? flag-fns arg)
        (flag-fns arg)
        (unrecognized! arg)))))

;; ## Parse

(defn- flag?
  "Check whether a string represents a flag."
  [^String s]
  (.startsWith s ":"))

(defn- split-args
  "Split command line arguments into flags and rest arguments."
  [args]
  (let [[fls rst] (split-with flag? args)]
    (->> (if (= (first rst) "--")
           (rest rst)
           rst)
         (vector fls))))

(defn parse
  "Parse the given command line args and produce a pair of
   options/remaining."
  [args & {:keys [change-defaults exclude]}]
  (let [exclude? (set (map str exclude))
        [fls rst] (split-args args)]
    (-> (reduce
          (fn [opts flag]
            (if-let [f (supported? flag exclude?)]
              (f opts)
              opts))
          (merge defaults change-defaults)
          fls)
    (vector rst))))

;; ## Documentation

(defn- caption
  [s]
  (->> (repeat (count s) \-)
       (apply str)
       (format "%n%n  %s%n  %s%n%n" s)))

(defn doc!
  "Attach CLI doc to var metadata."
  [task-var msg & {:keys [exclude]}]
  (->> (reduce dissoc flags exclude)
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
