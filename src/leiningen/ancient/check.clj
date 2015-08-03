(ns leiningen.ancient.check
  (:require [leiningen.ancient.artifact
             [options :as o]
             [files :as f]]
            [leiningen.ancient
             [collect :refer :all]
             [console :as console]
             [utils :as utils]
             [verbose :refer :all]]
            [leiningen.core.main :as main]))

;; ## Logic

(defn- check-file!
  [{:keys [artifact-opts]} file]
  (utils/with-success [r (f/read! file)]
    (utils/with-success [outdated (f/check! r artifact-opts)]
      (if (seq outdated)
        (doseq [artifact outdated]
          (console/print-outdated-message artifact))
        (verbosef "all artifacts are up-to-date."))
      {:outdated? (seq outdated)})))

(defn- exit-with-status
  [results]
  (when (some (comp :outdated? second) results)
    (main/exit 1)))

;; ## Tasks

(utils/deftask check
  {:docstring "Check projects for outdated artifacts. (default)"
   :exclude [:interactive :print :no-tests]}
  [opts]
  (exit-with-status
    (utils/call-on-project-files check-file! opts)))

(utils/deftask profiles
  {:docstring "Check profiles for outdated artifacts."
   :exclude [:no-profiles :interactive :print :no-tests :recursive]
   :fixed {:plugins? true}}
  [opts]
  (exit-with-status
    (utils/call-on-profiles-files check-file! opts)))
