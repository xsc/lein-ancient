(ns ^{ :doc "Rewrite project.clj to include latest versions of dependencies." 
       :author "Yannick Scherer" }
  leiningen.ancient.tasks.update-project
  (:require [leiningen.ancient.verbose :refer :all]
            [leiningen.ancient.maven-metadata :refer :all]
            [leiningen.ancient.maven-metadata all]  
            [leiningen.ancient.projects :refer [dependency-map]]
            [leiningen.ancient.version :refer [version-outdated?]]
            [leiningen.ancient.cli :refer [parse-cli]]
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
  [zloc retrieve! settings]
  (or 
    (let [prompt! (partial prompt-for settings)]
      (when (z/vector? zloc)
        (let [{:keys [group-id artifact-id version]} (dependency-map (z/sexpr zloc)) ]
          (when-let [mta (retrieve! group-id artifact-id)]
            (when-let [latest (latest-version mta settings)]
              (when (and (version-outdated? version latest)
                         (prompt! group-id artifact-id latest version))
                (println "Upgrading to" (artifact-string group-id artifact-id latest) "...")
                (z/edit-> zloc z/down z/right (z/replace (:version-str latest)))))))))
    zloc))

(defn- upgrade-dependencies!
  "Given a zipper node containing a vector of artifact vectors `[[artifact version] ...]`, perform
   upgrading mechanism on each one of them."
  [zloc retrieve! settings]
  (if-not (z/vector? zloc)
    zloc
    (z/map #(upgrade-dependency! % retrieve! settings) zloc)))

(defn- upgrade-project-key!
  "Given a zipper node containing the 'defproject' structure, find a given key and upgrade the
   dependencies following it."
  [retrieve! settings proj k]
  (or
    (when-let [n (z/find-value (z/down proj) k)]
      (when-let [v (z/right n)]
        (when-let [r (upgrade-dependencies! v retrieve! settings)]
          (z/up r))))
    proj))

(defn- upgrade-profiles-key!
  "Given a zipper node containing the 'defproject' structure, find a given key in each profile
   and upgrade the dependencies following it."
  [retrieve! settings proj k]
  (or
    (when-let [profiles-key (-> proj z/down (z/find-value :profiles))]
      (when-let [profiles (z/right profiles-key)]
        (when (z/map? profiles)
          (->> profiles
            (z/map 
              (fn [loc]
                (when (z/map? loc)
                  (z/edit-> loc 
                    (z/get k) 
                    (upgrade-dependencies! retrieve! settings)))))
            z/up))))
    proj))

(defn- upgrade-project!
  "Given a zipper representing the contents of the `project.clj` file, upgrade everytihng allowed 
   in the given settings map using the given retrievers."
  [retrieve! settings zloc]
  (let [upgrade-proj! (partial upgrade-project-key! retrieve! settings)
        upgrade-prof! (partial upgrade-profiles-key! retrieve! settings)
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
  [f retrieve! settings]
  (let [data (z/of-file f)]
    (when-let [data (upgrade-project! retrieve! settings data)]
      (if (:print settings)
        (do
          (println)
          (z/print-root data)
          (println))
        (binding [*out* (io/writer f)]
          (z/print-root data))))))

;; ## Task

(defn run-upgrade-task!
  "Run artifact upgrade on project file."
  [{:keys [root] :as project} args]
  (if-not root
    (println "':upgrade' can only be run inside of project.")
    (let [project-file (io/file root "project.clj")
          settings (parse-cli args)
          retrievers (collect-metadata-retrievers project)
          retrieve! (partial retrieve-metadata! retrievers settings)]
      (binding [*verbose* (:verbose settings)
                *colors* (not (:no-colors settings))]
        (upgrade-project-file! project-file retrieve! settings)))))
