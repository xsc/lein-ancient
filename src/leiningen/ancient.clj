(ns ^{:doc "Check your Project for outdated Dependencies."
      :author "Yannick Scherer"}
  leiningen.ancient
  (:use [leiningen.ancient.verbose :only [verbose *verbose* yellow green red *colors*]]
        [leiningen.ancient.maven-metadata :only [retrieve-metadata! latest-version version-seq filter-versions]]
        [leiningen.ancient.version :only [version-outdated? version-sort version-map snapshot? qualified?]]
        [leiningen.ancient.projects :only [collect-dependencies collect-metadata-retrievers dependency-map]])
  (:require [leiningen.ancient.maven-metadata http local s3p]))

;; ## Output Strings

(defn- version-string
  [version]
  (str "\"" (:version-str version) "\""))

(defn- artifact-string
  [group-id artifact-id version]
  (let [f (if (= group-id artifact-id)
            artifact-id
            (str group-id "/" artifact-id))]
    (str "[" f " " (green (version-string version)) "]")))

;; ## Actual Check Logic

(defn- check-packages
  "Check the packages found at the given key in the project map.
   Will check the given repository urls for metadata."
  [retrievers packages settings]
  (let [retrieve! (partial retrieve-metadata! retrievers settings)]
    (doseq [{:keys [group-id artifact-id version] :as dep} packages]
      (verbose "Checking " group-id "/" artifact-id " (current version: " (version-string version) ") ...")
      (if-let [mta (retrieve! group-id artifact-id)]
        (if-let [latest (latest-version mta settings)]
          (when (version-outdated? version latest)
            (println
              (artifact-string group-id artifact-id latest)
              "is available but we use"
              (yellow (version-string version))))
          (verbose "No latest Version found!"))
        (verbose "No Metadata File found!")))))

;; ## CLI

(def ^:private CLI_FLAGS
  "Available CLI Flags."
  #{":dependencies" ":all" ":plugins" ":allow-snapshots"
    ":allow-qualified" ":no-profiles" ":check-clojure"
    ":verbose" ":no-colors" ":aggressive"})

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

;; ## Operation Modes

;; ### default

(defn- run-default!
  "Run project/plugin checker."
  [project args]
  (let [settings (parse-cli args)]
    (binding [*verbose* (:verbose settings)
              *colors* (not (:no-colors settings))]
      (let [retrievers (collect-metadata-retrievers project)
            deps (collect-dependencies project settings)]
        (verbose "Checking " (count deps) " Dependencies using " (count retrievers) " Repositories ...")
        (check-packages retrievers deps settings)
        (verbose "Done.")))))

;; `:get`

(def ^:private ^:const WIDTH
  "Maximum width of labels for `:get`."
  23)

(defn- print-version
  "Print version number line."
  [label v]
  (when v
    (print (str "  * " label ": "))
    (print (apply str (repeat (- WIDTH (count label)) \space)))
    (print (green "\"" (:version-str v) "\""))
    (println)))

(defn- print-version-seq
  "Print version seq lines."
  [label vs]
  (when (seq vs)
    (let [c (count label)
          indent (apply str (repeat (+ 8 WIDTH) \space))
          label (if (>= c WIDTH) (str label ":") (str label ":" (apply str (repeat (- WIDTH c) \space))))]
      (print (str "  * " label " [ "))
      (let [vps (partition 5 5 nil (map :version-str vs))]
        (doseq [v (first vps)] (print (pr-str v) ""))
        (doseq [vp (rest vps)] 
          (println)
          (print indent)
          (doseq [v vp] (print (pr-str v) ""))))
      (println "]"))))

(defn- run-get!
  [project [_ package & args]]
  (if-not package
    (println "':get' expects a package to retrieve version information for.")
    (let [settings (-> (parse-cli args)
                     (assoc :aggressive true))
          {:keys [artifact-id group-id]} (dependency-map [package ""])
          artifact-str (str group-id "/" artifact-id)]
      (binding [*verbose* (:verbose settings)
                *colors* (not (:no-colors settings))]
        (let [retrievers (collect-metadata-retrievers project)]
          (println "Getting Version Information for" (yellow artifact-str) 
                   "from" (count retrievers) "Repositories ...")
          (let [vs (->> (retrieve-metadata! retrievers settings group-id artifact-id)
                     (mapcat version-seq)
                     (distinct)
                     (map version-map)
                     (version-sort)
                     (reverse))]
            (if-not (seq vs)
              (println "No versions found.")
              (let [releases (filter (complement qualified?) vs)
                    snapshots (filter snapshot? vs)
                    qualified (filter #(and (not (snapshot? %)) (qualified? %)) vs)]
                (println (str "  * " (count vs) " version(s) found."))
                (print-version "latest release" (first releases))
                (print-version "latest SNAPSHOT" (first snapshots))
                (print-version "latest qualified" (first qualified))
                (print-version-seq "all releases" releases)
                (print-version-seq "all SNAPSHOTs" snapshots)
                (print-version-seq "all qualified versions" qualified)))))))))

;; ## Main

(defn ^:no-project-needed ancient
  "Check your Projects for outdated Dependencies. 
  
   Usage:

     lein ancient :get <package> [<options>]
     lein ancient [<options>]

   Commandline Options:
  
     :all                 Check Dependencies and Plugins.
     :dependencies        Check Dependencies. (default)
     :plugins             Check Plugins.
     :no-profiles         Do not check Dependencies/Plugins in Profiles.
     :allow-qualified     Allow '*-alpha*' versions & co. to be reported as new.
     :allow-snapshots     Allow '*-SNAPSHOT' versions to be reported as new.
     :check-clojure       Include Clojure (org.clojure/clojure) in checks.
     :aggressive          Check all available repositories (= Do not stop after first artifact match).
     :verbose             Produce progress indicating messages.
     :no-colors           Disable colorized output.
  "
  [project & args]
  (condp = (first args)
    ":get" (run-get! project args)
    (run-default! project args)))
