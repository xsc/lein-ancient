(ns ^{:doc "Check your Project for outdated Dependencies."
      :author "Yannick Scherer"}
  leiningen.ancient
  (:use [leiningen.ancient.verbose :only [verbose *verbose* yellow green red *colors*]]
        [leiningen.ancient.maven-metadata :only [retrieve-metadata! latest-version]]
        [leiningen.ancient.version :only [version-outdated?]]
        [leiningen.ancient.projects :only [collect-dependencies collect-metadata-retrievers]])
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

(defn ^:no-project-needed ancient
  "Check your Projects for outdated Dependencies. 
   
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
  (let [settings (parse-cli args)]
    (binding [*verbose* (:verbose settings)
              *colors* (not (:no-colors settings))]
      (let [retrievers (collect-metadata-retrievers project)
            deps (collect-dependencies project settings)]
        (verbose "Checking " (count deps) " Dependencies using " (count retrievers) " Repositories ...")
        (check-packages retrievers deps settings)
        (verbose "Done.")))))
