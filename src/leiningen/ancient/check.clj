(ns ^{ :doc "Check for outdated dependencies." 
       :author "Yannick Scherer" }
  leiningen.ancient.check
  (:require [leiningen.ancient.utils.projects :refer :all]
            [leiningen.ancient.utils.cli :refer :all]
            [leiningen.ancient.utils.io :refer :all]
            [leiningen.core.main :as main]
            [ancient-clj.verbose :refer :all]
            [ancient-clj.core :as anc]
            [clojure.java.io :as io]))

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

;; ## Check Logic

(defn- check-project-map!
  "Run project/plugin checker on the given project/settings maps."
  [project settings]
  (with-output-settings settings
    (let [repos (collect-repositories project)
          artifacts (collect-artifacts project settings)
          outdated (get-outdated-artifacts! repos settings artifacts)]
      (verbose "Checking " (count artifacts) " Dependencies using " (count repos) " Repositories ...")
      (doseq [artifact outdated]
        (print-outdated-message artifact))
      (verbose "Done."))))

(defn- check-profiles-file!
  "Check a single profiles file."
  [repositories path settings]
  (when-let [profiles (read-profiles-map! path)]
    (check-project-map! {:repositories repositories :profiles profiles}
                        (-> settings
                          (assoc :profiles true)
                          (assoc :plugins true)))))

(defn- check-project-file!
  "Check a single project file."
  [path settings]
  (when-let [project (read-project-map! path)]
    (when (:recursive settings) (main/info "--" path))
    (check-project-map! project settings)
    (when (:recursive settings) (main/info))))

(defn- check-project-directory!
  "Check files in directory, possibly recursively."
  [path settings]
  (if (:recursive settings)
    (let [project-files (find-files-recursive! path "project.clj")]
      (doseq [^java.io.File project-file project-files]
        (prn project-file)
        (check-project-file! (.getPath project-file) settings)))
    (check-project-file! (.getPath (io/file path "project.clj")) settings)))

(defn- check-path!
  "Run project/plugin checker on the given path."
  [repositories path args]
  (let [^java.io.File f (io/file path)
        settings (parse-cli args)]
    (cond (.isFile f) (cond (= (.getName f) "project.clj") (check-project-file! path settings)
                            (= (.getName f) "profiles.clj") (check-profiles-file! repositories path settings)
                            :else (main/abort "Can only check 'project.clj' or 'profiles.clj'."))
          (.isDirectory f) (check-project-directory! path settings)
          :else (main/abort "No such file or directory:" path))))

;; Tasks

(defn run-check-task!
  "Run project/plugin checker."
  [{:keys [repositories root]} args]
  (if (exists? (last args))
    (check-path! repositories (last args) (butlast args))
    (check-path! repositories (or root ".") args)))

(defn run-profiles-task!
  "Run plugin checker on global profiles file."
  [{:keys [repositories]} args]
  (let [f (java.io.File. (System/getProperty "user.home") ".lein/profiles.clj")]
    (if (.isFile f)
      (check-path! repositories f args)
      (main/info "No file at: ~/.lein/profiles.clj"))))
