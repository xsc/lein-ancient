(ns ^{ :doc "Rewrite project.clj to include latest versions of dependencies." 
       :author "Yannick Scherer" }
  leiningen.ancient.tasks.upgrade
  (:require [leiningen.ancient.projects :refer [collect-repositories]]
            [leiningen.ancient.cli :refer [parse-cli]]
            [ancient-clj.verbose :refer :all]
            [ancient-clj.core :as anc]
            [rewrite-clj.zip :as z]
            [clojure.java.io :as io :only [file writer]]))

;; ## Prompt

(defn- prompt
  "Create a yes/no prompt using the given message."
  [& msg]
  (let [msg (str (apply str msg) " [yes/no] ")]
    (loop [i 3]
      (when (pos? i)
        (print msg)
        (.flush ^java.io.Writer *out*)
        (let [r (or (read-line) "")
              r (.toLowerCase r)]
          (cond (= r "yes") true
                (= r "no") false
                :else (recur (dec i))))))))

(defn- prompt-for-upgrade 
  "If the `:interactive` flag in the given settings map is set, this function will ask the
   user (on stdout/stdin) whether he wants to upgrade the given artifact and return a boolean
   value indicating the user's choice."
  [settings group-id artifact-id latest version]
  (or 
    (not (:interactive settings))
    (do
      (println)
      (println (artifact-string group-id artifact-id latest) 
               "is available but we use"
               (yellow (version-string version)))
      (prompt "Do you want to upgrade?"))))

;; ## Upgrade

(defn- upgrade-dependency!
  "Given a zipper node containing a single artifact's dependency vector `[artifact version]`
   check if newer versions are available and replace if so."
  [zloc repos settings]
  (or 
    (let [prompt! (partial prompt-for-upgrade settings)]
      (when (z/vector? zloc)
        (let [{:keys [group-id artifact-id version] :as artifact} (anc/artifact-map (z/sexpr zloc)) ]
          (when (or (:check-clojure settings)
                    (not= (str group-id "/" artifact-id) "org.clojure/clojure"))
            (when-let [latest (anc/artifact-outdated? settings repos artifact)]
              (when (prompt! group-id artifact-id latest version)
                (println "Upgrade to" (artifact-string group-id artifact-id latest) 
                         "from" (yellow (version-string version)))
                (z/subedit-> zloc z/down z/right (z/replace (first latest)))))))))
    zloc))

(defn- upgrade-dependencies!
  "Given a zipper node containing a vector of artifact vectors `[[artifact version] ...]`, perform
   upgrading mechanism on each one of them."
  [zloc repos settings]
  (if-not (z/vector? zloc)
    zloc
    (z/map #(upgrade-dependency! % repos settings) zloc)))

(defn- upgrade-project-key!
  "Given a zipper node containing the 'defproject' structure, find a given key and upgrade the
   dependencies following it."
  [repos settings proj k]
  (or
    (when-let [n (z/find-value (z/down proj) k)]
      (when-let [v (z/right n)]
        (when-let [r (upgrade-dependencies! v repos settings)]
          (z/up r))))
    proj))

(defn- upgrade-profiles-key!
  "Given a zipper node containing the 'defproject' structure, find a given key in each profile
   and upgrade the dependencies following it."
  [repos settings proj k]
  (or
    (when-let [profiles-key (-> proj z/down (z/find-value :profiles))]
      (when-let [profiles (z/right profiles-key)]
        (when (z/map? profiles)
          (->> profiles
            (z/map 
              (fn [loc]
                (when (z/map? loc)
                  (if-let [n (z/get loc k)]
                    (z/up (upgrade-dependencies! n repos settings))
                    loc))))
            z/up))))
    proj))

(defn- upgrade-project!
  "Given a zipper representing the contents of the `project.clj` file, upgrade everytihng allowed 
   in the given settings map using the given retrievers."
  [repos settings zloc]
  (let [upgrade-proj! (partial upgrade-project-key! repos settings)
        upgrade-prof! (partial upgrade-profiles-key! repos settings)
        deps? (:dependencies settings)
        plugins? (:plugins settings)
        upgrading-steps (concat
                          (when deps? [#(upgrade-proj! % :dependencies)])
                          (when plugins? [#(upgrade-proj! % :plugins)])
                          (when-not (:no-profiles settings)
                            (concat
                              (when deps? [#(upgrade-prof! % :dependencies)])
                              (when plugins? [#(upgrade-prof! % :plugins)]))))]
    (or
      (if-let [proj (z/find-value zloc z/next 'defproject)]
        (reduce 
          (fn [proj f] 
            (f proj)) 
          (z/up proj) upgrading-steps)
        (println "Could not find valid project map '(defproject ...)'."))
      zloc)))

(defn- upgrade-profiles-plugins!
  "Given a zipper location containing a map of profiles, update the profile plugins."
  [repos settings zloc]
  (when-let [zloc (z/find-tag zloc z/next :map)] 
    (z/map
      (fn [loc]
        (or
          (when (z/map? loc)
            (when-let [plugins (z/get loc :plugins)]
              (-> plugins 
                (upgrade-dependencies! repos settings)
                z/up)))
          loc))
      zloc)))

;; ## Handle Files

(defn- create-backup-file!
  "Create backup of a given File. Print errors and return `nil` if a failure occurs."
  [^java.io.File f]
  (let [^java.io.File parent (.getParentFile f)
        ^java.io.File backup (io/file parent (str (.getName f) ".backup"))]
    (try
      (when (or (not (.exists backup))
                (prompt "Do you want to overwrite the existing backup file?")) 
        (verbose "Creating backup at: " (.getCanonicalPath backup))
        (io/copy f backup) 
        backup)
      (catch Exception ex
        (println (red "Could not create backup file:") " " (.getMessage ex))
        nil))))

(defn- read-clojure-file!
  "Read Clojure File. Print errors and return `nil` if a failure occurs."
  [^java.io.File f]
  (try
    (z/of-file f)
    (catch Exception ex
      (println (red "Could not read artifacts from file:") " " (.getMessage ex))
      nil)))

(defn- upgrade-file-with-backup!
  "Given a Clojure file and an upgrade function, upgrade everything allowed
   in the given settings map using the given repositories and write result back
   to file, creating a backup file first."
  [upgrade-fn ^java.io.File f repos settings]
  (verbose "Upgrading artifacts in: " (.getCanonicalPath f))
  (when-let [backup (create-backup-file! f)]
    (when-let [src-data (read-clojure-file! f)]
      (when-let [data (upgrade-fn repos settings src-data)]
        (if (:print settings)
          (do
            (println)
            (z/print-root data)
            (println))
          (binding [*out* (io/writer f)]
            (z/print-root data)
            (.flush ^java.io.Writer *out*)))))))

(defn- upgrade-file!
  "Given a Clojure file (as `java.io.File`) and a upgrade function, upgrade everything allowed in 
   the given settings map using the given retrievers and write result back to the file."
  [upgrade-fn ^java.io.File f repos settings]
  (if-not (and (.isFile f) (.exists f))
    (println "No such file:" (.getPath f))
    (upgrade-file-with-backup! upgrade-fn f repos settings)))

(def ^:private upgrade-project-file! 
  (partial upgrade-file! upgrade-project!))

(def ^:private upgrade-profiles-file!
  (partial upgrade-file! upgrade-profiles-plugins!))

;; ## Task

(defn run-upgrade-task!
  "Run artifact upgrade on project file."
  [{:keys [root] :as project} args]
  (if-not root
    (println "'upgrade' can only be run inside of project.")
    (let [project-file (io/file root "project.clj")
          settings (parse-cli args)
          repos (collect-repositories project)]
      (with-settings settings
        (upgrade-project-file! project-file repos settings)))))

(defn run-upgrade-global-task!
  "Run plugin upgrade on global profiles."
  [project args]
  (let [profiles-file (io/file (System/getProperty "user.home") ".lein" "profiles.clj")
        settings (parse-cli args)
        repos (collect-repositories project)]
    (with-settings settings
      (upgrade-profiles-file! profiles-file repos settings))))
