(ns ^{ :doc "Check for outdated dependencies." 
       :author "Yannick Scherer" }
  leiningen.ancient.check
  (:require [leiningen.ancient.utils.projects :refer [collect-artifacts collect-repositories]]
            [leiningen.ancient.utils.cli :refer [parse-cli]]
            [ancient-clj.verbose :refer :all]
            [ancient-clj.core :as anc]))

;; ## Actual Check Logic

(defn- check-artifacts
  "Check the given artifacts for outdated dependencies."
  [repos settings artifacts]
  (doseq [{:keys [group-id artifact-id version] :as artifact} artifacts]
    (verbose "Checking " group-id "/" artifact-id " (current version: " (version-string version) ") ...")
    (when-let [latest (anc/artifact-outdated? settings repos artifact)]
      (println
        (artifact-string group-id artifact-id latest)
        "is available but we use"
        (yellow (version-string version))))))

;; ## Task

(defn run-check-task!
  "Run project/plugin checker."
  [project args]
  (let [settings (parse-cli args)]
    (with-settings settings
      (let [repos (collect-repositories project)
            artifacts (collect-artifacts project settings)]
        (verbose "Checking " (count artifacts) " Dependencies using " (count repos) " Repositories ...")
        (check-artifacts repos settings artifacts)
        (verbose "Done.")))))
