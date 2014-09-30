(ns ancient-clj.io.local
  (:require [ancient-clj.io.xml :as xml]
            [clojure.java.io :as io])
  (:import [java.io File]))

(defn local-loader
  "Create a version loader for a local repository."
  [path & [{:keys [filenames]}]]
  (let [dir (io/file path)]
    (if (.isDirectory dir)
      (fn [group id]
        (->> ["maven-metadata-local.xml"
              "maven-metadata-clojars.xml"]
             (or filenames)
             (cons nil)
             (map (partial xml/metadata-path group id))
             (map (partial io/file dir))
             (filter #(.isFile ^File %))
             (mapcat (comp xml/metadata-xml->versions slurp))))
      (constantly []))))
