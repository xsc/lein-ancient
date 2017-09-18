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
          (if (:include? artifact)
            (console/print-outdated-message artifact)
            (console/print-ignored-message artifact)))
        (verbosef "all artifacts are up-to-date."))
      {:outdated? (seq outdated)})))

(defn- exit-with-status
  [results]
  (when (->> (mapcat (comp :outdated? second) results)
             (filter :include?)
             (seq))
    (main/exit 1)))

;; ## Tasks

(utils/deftask check
  {:docstring "Check projects for outdated artifacts. (default)"
   :exclude [:interactive :print :no-tests]}
  [opts]
  (exit-with-status
    (utils/call-on-project-files check-file! opts)))

(utils/deftask check-profiles
  {:docstring "Check profiles for outdated artifacts."
   :exclude [:no-profiles :interactive :print :no-tests :recursive]
   :fixed {:plugins? true}}
  [opts]
  (exit-with-status
    (utils/call-on-profiles-files check-file! opts)))

(utils/deftask check-stdin
  {:docstring "Check artifacts in list(s) provided via stdin."
   :exclude   [:interactive :print :no-tests :recursive]}
  [opts]
  (when-not (utils/stream-available? System/in)
    (println "please specify artifacts list(s) on stdin.")
    (main/exit 1))
  (if-some [input (utils/read-edn-forms-from-stream *in*)]
    (let [artifacts-list (vec (apply concat input))
          fake-project-map {:dependencies artifacts-list}
          fake-file (f/virtual-file "<stdin>" fake-project-map)]
      (debugf "input artifacts list: %s" artifacts-list)
      (exit-with-status
        (utils/call-file check-file! opts fake-file)))
    (main/exit 2)))
