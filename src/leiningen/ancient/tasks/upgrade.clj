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

(defn- prompt-for 
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
      (loop []
        (print "Do you want to upgrade? [yes/no] ")
        (.flush ^java.io.Writer *out*)
        (let [r (read-line)]
          (cond (= r "yes") true
                (= r "no") false
                :else (recur)))))))

;; ## Upgrade

(defn- upgrade-dependency!
  "Given a zipper node containing a single artifact's dependency vector `[artifact version]`
   check if newer versions are available and replace if so."
  [zloc repos settings]
  (or 
    (let [prompt! (partial prompt-for settings)]
      (when (z/vector? zloc)
        (let [{:keys [group-id artifact-id version] :as artifact} (anc/artifact-map (z/sexpr zloc)) ]
          (when (or (:check-clojure settings)
                    (not= (str group-id "/" artifact-id) "org.clojure/clojure"))
            (when-let [latest (anc/artifact-outdated? settings repos artifact)]
              (when (prompt! group-id artifact-id latest version)
                (println "Upgrade to" (artifact-string group-id artifact-id latest) 
                         "from" (yellow (version-string version)))
                (z/edit-> zloc z/down z/right (z/replace (first latest)))))))))
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

(defn- upgrade-project-file!
  "Given a project's `project.clj` file (as `java.io.File`), upgrade everything allowed in 
   the given settings map using the given retrievers and write result back to the file."
  [f repos settings]
  (let [data (z/of-file f)]
    (when-let [data (upgrade-project! repos settings data)]
      (if (:print settings)
        (do
          (println)
          (z/print-root data)
          (println))
        (binding [*out* (io/writer f)]
          (z/print-root data)
          (.flush ^java.io.Writer *out*))))))

;; ## Task

(defn run-upgrade-task!
  "Run artifact upgrade on project file."
  [{:keys [root] :as project} args]
  (if-not root
    (println "':upgrade' can only be run inside of project.")
    (let [project-file (io/file root "project.clj")
          settings (parse-cli args)
          repos (collect-repositories project)]
      (with-settings settings
        (upgrade-project-file! project-file repos settings)))))
