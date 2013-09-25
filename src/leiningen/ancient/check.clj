(ns ^{ :doc "Check for outdated dependencies." 
       :author "Yannick Scherer" }
  leiningen.ancient.check
  (:require [leiningen.ancient.utils.projects :refer :all]
            [leiningen.ancient.utils.cli :refer :all]
            [leiningen.ancient.utils.io :refer :all]
            [leiningen.core.main :as main]
            [ancient-clj.verbose :refer :all]
            [ancient-clj.core :as anc]))

;; ## Check/Update

(defn check-artifact!
  "Checks the given artifact and stores the latest version in `:latest`."
  [repos settings {:keys [group-id artifact-id version] :as artifact}]
  (let [path (artifact-path artifact)]
    (verbose "Checking " group-id "/" artifact-id " (current version: " (version-string version) 
             " at " (artifact-path artifact) ") ...")
    (assoc artifact :latest (anc/artifact-outdated? settings repos artifact))))

(defn get-outdated-artifacts!
  "Get (a lazy seq of) all outdated artifacts (with the latest version in `:latest`).
   Input and Output are artifact maps."
  [repos settings artifacts]
  (let [checked-artifacts (map #(check-artifact! repos settings %) artifacts)]
    (filter :latest checked-artifacts)))

;; ## Console Interaction

(defn print-outdated-message
  "Takes an artifact map (containing the `:latest` element) and prints 
   the \"[...] is available but we use ...\" message to stdout."
  [{:keys [group-id artifact-id version latest]}]
  (main/info
    (artifact-string group-id artifact-id latest)
    "is available but we use"
    (yellow (version-string version))))

;; ## Task

(defn run-check-task!
  "Run project/plugin checker."
  [project args]
  (let [settings (parse-cli args)]
    (with-output-settings settings
      (let [repos (collect-repositories project)
            artifacts (collect-artifacts project settings)
            outdated (get-outdated-artifacts! repos settings artifacts)]
        (verbose "Checking " (count artifacts) " Dependencies using " (count repos) " Repositories ...")
        (doseq [artifact outdated]
          (print-outdated-message artifact))
        (verbose "Done.")))))

(defn run-file-check-task!
  "Run project/plugin checker on the given file."
  [project [path & args]]
  (when-let [project (read-project-map! path)]
    (run-check-task! project args)))
