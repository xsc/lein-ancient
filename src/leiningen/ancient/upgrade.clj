(ns leiningen.ancient.upgrade
  (:require [leiningen.ancient.artifact
             [reader :as reader]
             [options :as o]
             [files :as f]]
            [leiningen.ancient
             [collect :refer :all]
             [console :as console]
             [test :as t]
             [utils :as utils]
             [verbose :refer :all]]
            [clojure.pprint :as pp]
            [clojure.java.io :as io]))

;; ## Logic

(defn- prompt-for-upgrade!
  [artifact {:keys [opts]}]
  (let [result (if (:include? artifact)
                 (do
                   (console/print-outdated-message artifact)
                   (or (not (:interactive? opts))
                       (console/prompt "Do you want to upgrade?")))
                 (console/print-ignored-message artifact))]
    (println)
    result))

(defn- as-string
  [upgraded]
  (with-open [w (java.io.StringWriter.)]
    (f/write! upgraded w)
    (.trim (.toString w))))

(defn- as-project-map
  [upgraded]
  (utils/with-temp-file [f (as-string upgraded)]
    (let [root (->> (:path upgraded)
                    (.getParentFile)
                    (.getCanonicalPath))]
      (reader/read-project-for-tests! root f))))

(defn- write-back!
  [upgraded {:keys [opts]}]
  (if (:print? opts)
    (do
      (printf "%n%s%n" (as-string upgraded))
      (flush))
    (do
      (f/write-out! upgraded)
      upgraded)))

(defn- report!
  [upgraded outdated upgradable]
  (let [o (count outdated)
        u (count upgradable)]
    (verbosef "%d/%d artifacts were upgraded." u o)
    {:outdated o, :upgraded u}))

(defn- test!
  [upgraded {:keys [opts]}]
  (if (:tests? opts)
    (let [t (t/run-tests! (as-project-map upgraded))]
      (if (utils/throwable? t)
        (do
          (errorf "tests have failed (use ':no-tests' to disable testing).")
          t)
        upgraded))
    (do
      (debugf "tests are disabled.")
      upgraded)))

(defn- upgrade-file!
  [{:keys [artifact-opts] :as opts} file]
  (utils/with-success [r (f/read! file)]
    (utils/with-success [outdated (f/check! r artifact-opts)]
      (if (seq outdated)
        (let [upgradable (filter
                           #(prompt-for-upgrade! % opts)
                           outdated)]
          (utils/with-success [upgraded (f/upgrade! r upgradable)]
            (utils/with-success [tested (test! upgraded opts)]
              (some-> tested
                      (write-back! opts)
                      (report! outdated upgradable)))))
        (verbosef "nothing to upgrade.")))))

;; ## Tasks

(utils/deftask upgrade
  {:docstring "Upgrade all project artifacts."
   :fixed {:report? true}}
  [opts]
  (utils/call-on-project-files upgrade-file! opts))

(utils/deftask upgrade-profiles
  {:docstring "Upgrade all artifacts in the global and local profiles."
   :exclude [:no-profiles :no-tests :tests]
   :fixed {:plugins? true
           :profiles? true
           :tests? false
           :report? true}}
  [opts]
  (utils/call-on-profiles-files upgrade-file! opts))
