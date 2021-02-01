(ns leiningen.ancient.collect
  (:require [leiningen.ancient.artifact.files :as f]
            [clojure.java.io :as io])
  (:import [java.io File]
           [org.apache.commons.io FileUtils]))

;; ## Project

(defn project-file-at
  "Find project file in the given directory.
   Returns `nil` "
  [path]
  (let [f (io/file path)]
    (if (.isDirectory f)
      (->> (io/file f "project.clj")
           (f/project-file))
      (if (.isFile f)
        (f/project-file f)))))

(defn current-project-file
  "Find the current project file"
  [project]
  (project-file-at (:root project)))

(defn recursive-project-files
  "Find all project files in a directory and its children."
  [path]
  (letfn [(lazy-find [^java.io.File dir]
            (lazy-seq
              (when-not (FileUtils/isSymlink dir)
                (when (.isDirectory dir)
                  (let [f (io/file dir "project.clj")
                        rst (filter #(.isDirectory ^java.io.File %) (.listFiles dir))]
                    (if (.isFile f)
                      (cons f (mapcat lazy-find rst))
                      (mapcat lazy-find rst)))))))]
    (->> (io/file path)
         (.getCanonicalPath)
         (io/file)
         (lazy-find)
         (map f/project-file))))

;; ## Profiles

(defn- lein-home
  "Get the Leiningen home directory."
  []
  (or (if-let [p (System/getenv "LEIN_HOME")]
        (let [f (io/file p)]
          (if (.isDirectory f)
            f)))
      (let [home (System/getProperty "user.home")]
        (io/file home ".lein"))))

(defn- project-profiles-file
  [project]
  (io/file (:root project) "profiles.clj"))

(defn- default-profiles-file
  "Get the default profiles.clj."
  []
  (io/file (lein-home) "profiles.clj"))

(defn- profiles-directory-files
  "Get the sub-profiles directory."
  []
  (let [d (io/file (lein-home) "profiles.d")]
    (keep
      (fn [^File f]
        (let [n (.getName f)]
          (if (or (.endsWith n ".clj")
                  (.endsWith n ".edn"))
            (-> (subs n 0 (- (count n) 4))
                (keyword)
                (vector)
                (vector f)))))
      (.listFiles d))))

(defn profiles-file-at
  [path]
  (let [f (io/file path)]
    (if (.isDirectory f)
      (->> (io/file f "profiles.clj")
           (f/profiles-file))
      (if (.isFile f)
        (f/profiles-file f)))))

(defn profiles-files
  "Collect all profiles files."
  [project]
  (->> (profiles-directory-files)
       (cons [[] (default-profiles-file)])
       (cons [[] (project-profiles-file project)])
       (keep
         (fn [[prefix ^File f]]
           (if (.isFile f)
             (->> (cons :profiles prefix)
                  (vec)
                  (f/profiles-file f)))))))
