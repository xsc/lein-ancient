(ns leiningen.ancient.check
  (:require [leiningen.ancient.artifact
             [options :as o]
             [files :as f]]
            [leiningen.ancient
             [collect :refer :all]
             [console :as console]
             [utils :as utils]
             [verbose :refer :all]]))

;; ## Logic

(defn- check-file!
  [{:keys [artifact-opts]} file]
  (utils/with-success [r (f/read! file)]
    (utils/with-success [outdated (f/check! r artifact-opts)]
      (if (seq outdated)
        (doseq [artifact outdated]
          (console/print-outdated-message artifact))
        (verbosef "all artifacts are up-to-date.")))))

;; ## Tasks

(utils/deftask check
  {:docstring "Check projects for outdated artifacts. (default)"
   :exclude [:interactive :print :no-tests]}
  [opts]
  (utils/call-on-project-files check-file! opts))

(utils/deftask profiles
  {:docstring "Check profiles for outdated artifacts."
   :exclude [:no-profiles :interactive :print :no-tests :recursive]
   :fixed {:plugins? true}}
  [opts]
  (utils/call-on-profiles-files check-file! opts))
