(ns ^{ :doc "Check for outdated dependencies." 
       :author "Yannick Scherer" }
  leiningen.ancient.check
  (:require [leiningen.ancient.utils.projects :refer [collect-artifacts 
                                                      artifact-path 
                                                      collect-repositories]]
            [leiningen.ancient.utils.cli :refer [parse-cli]]
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

;; ## Task

(defn run-check-task!
  "Run project/plugin checker."
  [project args]
  (let [settings (parse-cli args)]
    (with-settings settings
      (let [repos (collect-repositories project)
            artifacts (collect-artifacts project settings)
            outdated (get-outdated-artifacts! repos settings artifacts)]
        (verbose "Checking " (count artifacts) " Dependencies using " (count repos) " Repositories ...")
        (doseq [{:keys [group-id artifact-id version latest]} outdated]
          (println
            (artifact-string group-id artifact-id latest)
            "is available but we use"
            (yellow (version-string version))))
        (verbose "Done.")))))
