(ns leiningen.ancient.artifact.reader
  (:require [leiningen.core
             [main :as main]
             project]
            [clojure.tools.reader :as reader]
            [clojure.java.io :as io]))

(defn read-project-map!
  "Read project map from given file."
  [path]
  (let [f (io/file path)]
    (locking read-project-map!
      (binding [*ns* (find-ns 'leiningen.core.project)]
        (load-file (.getCanonicalPath f))
        (when-let [project (resolve 'leiningen.core.project/project)]
          (ns-unmap 'leiningen.core.project 'project)
          @project)))))

(defn read-profiles-map!
  [path prefix]
  (when-let [form (reader/read-string (slurp path))]
    (when-not (map? form)
      (throw (Exception. (str "Not a map: " form))))
    (reduce #(hash-map %2 %1) form (reverse prefix))))
