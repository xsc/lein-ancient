(ns leiningen.ancient.artifact.reader
  (:require [leiningen.core
             [main :as main]
             [project :as project]]
            [rewrite-clj.zip :as z]
            [clojure.tools.reader :as reader]
            [clojure.java.io :as io])
  (:import [java.io File]))

(defn- find-in-project
  [root k default]
  (or (some-> (z/find-value root k)
              (z/right)
              (z/sexpr))
      default))

(defn read-project-map!
  "Read project map from given file."
  [path]
  (if-let [root (some-> (z/of-file path)
                        (z/find-value z/next 'defproject))]
    {:dependencies         (find-in-project root :dependencies [])
     :managed-dependencies (find-in-project root :managed-dependencies [])
     :plugins              (find-in-project root :plugins [])
     :profiles             (find-in-project root :profiles {})}
    (throw
      (Exception.
        (str "invalid project file: " path)))))

(defn read-project!
  [^File f & [only]]
  (let [root (.getCanonicalPath f)]
    (or (when (.isFile f)
          (when-let [m (project/read root)]
            (if (empty? only)
              m
              (select-keys m only))))
        {:root root})))

(defn read-project-for-tests!
  "Read project map using the `defproject` macro. Adjust all paths using
   the given root."
  [root project-file]
  (let [old-project (read-project!
                      (io/file root "project.clj"))
        new-project (read-project!
                      (io/file project-file)
                      [:managed-dependencies
                       :dependencies
                       :plugins
                       :profiles])]
    (project/init-project
      (with-meta
        (merge old-project new-project)
        {}))))

(defn read-profiles-map!
  [path prefix]
  (when-let [form (reader/read-string (slurp path))]
    (when-not (map? form)
      (throw (Exception. (str "Not a map: " form))))
    (reduce #(hash-map %2 %1) form (reverse prefix))))
