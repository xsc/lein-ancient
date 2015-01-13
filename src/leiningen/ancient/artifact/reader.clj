(ns leiningen.ancient.artifact.reader
  (:require [leiningen.core
             [main :as main]
             [project :as project]]
            [rewrite-clj.zip :as z]
            [clojure.tools.reader :as reader]
            [clojure.java.io :as io]))

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
    {:dependencies (find-in-project root :dependencies [])
     :plugins (find-in-project root :plugins [])
     :profiles (find-in-project root :profiles {})}
    (throw
      (Exception.
        (str "invalid project file: " path)))))

(defn read-project!
  "Read project map using the `defproject` macro."
  [path]
  (let [f (io/file path)]
    (if (.isFile f)
      (project/read (.getCanonicalPath f))
      (throw
        (Exception.
          (str "not a project file: " path))))))

(defn read-profiles-map!
  [path prefix]
  (when-let [form (reader/read-string (slurp path))]
    (when-not (map? form)
      (throw (Exception. (str "Not a map: " form))))
    (reduce #(hash-map %2 %1) form (reverse prefix))))
