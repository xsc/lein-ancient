(ns ^{:doc "Version Utilities for lein-ancient"
      :author "Yannick Scherer"}
  leiningen.ancient.version
  (:require [version-clj.core :as v]))

;; ## Version Map

(defn version-map
  "Create version map from version string. Uses everything after the first
  non-number-or-dot character as `:qualifier`, storing a vector of version numbers
  in `:version`."
  [^String version]
  (-> {}
    (assoc :version-str version)
    (assoc :version (v/version->seq version))))

(defn- version-map-compare
  "Compare two version maps."
  [m0 m1]
  (v/version-seq-compare (:version m0) (:version m1)))

(defn version-outdated?
  "Check if the first version is outdated."
  [v0 v1]
  (= -1 (version-map-compare v0 v1)))

(defn version-sort 
  "Sort Version Maps."
  [version-maps]
  (sort version-map-compare version-maps))

(defn snapshot?
  "Check if the given version is a SNAPSHOT."
  [v]
  (some
    (fn [x]
      (cond (string? x) (= x "snapshot")
            (integer? x) nil
            :else (snapshot? {:version x})))
    (:version v)))

(defn qualified?
  "Check if the given version is a qualified version (i.e. contains a string element)."
  [v]
  (some
    (fn [x]
      (cond (string? x) (not= x "")
            (integer? x) nil
            :else (qualified? {:version x})))
    (:version v)) )
