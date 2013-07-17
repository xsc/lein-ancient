(ns ^{ :doc "Check for outdated dependencies." 
       :author "Yannick Scherer" }
  leiningen.ancient.tasks.check
  (:require [leiningen.ancient.verbose :refer :all]
            [leiningen.ancient.maven-metadata :refer :all]
            [leiningen.ancient.maven-metadata http local s3p]  
            [leiningen.ancient.version :refer [version-outdated?]]
            [leiningen.ancient.projects :refer [collect-dependencies repository-maps dependency-map]]
            [leiningen.ancient.cli :refer [parse-cli]]))

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

;; ## Task

(defn run-check-task!
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
