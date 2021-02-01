(ns leiningen.ancient.artifact.zip
  (:require [leiningen.ancient.verbose :refer :all]
            [rewrite-clj.zip :as z]
            [clojure.java.io :as io]))

;; ## Zipper IO

(defn read-project-zipper!
  "Read rewrite-clj zipper from project file."
  [path]
  (when-let [loc (z/of-file path)]
    (z/find-value loc z/next 'defproject)))

(defn read-profiles-zipper!
  [path]
  "Read rewrite-clj zipper from profiles file."
  (when-let [loc (z/of-file path)]
    (some-> loc
            (z/find-tag z/next :map)
            z/down)))

(defn write-zipper!
  "Write zipper to Writer."
  [zloc ^java.io.Writer writer]
  (let [s (z/->root-string zloc)]
    (.write writer s)))

;; ## Upgrade

(defn- move-through-meta
  [zloc]
  (if (and zloc (#{:meta :meta*} (z/tag zloc)))
    (recur (some-> zloc z/down z/right))
    zloc))

(defn- move-up-through-meta
  [zloc]
  (let [zloc (z/up zloc)]
    (if (and zloc (#{:meta :meta*} (z/tag zloc)))
      (recur zloc)
      zloc)))

(defn- move-to-index
  [zloc p]
  (move-through-meta
    (nth
      (->> (iterate z/right zloc)
           (remove #(= (z/tag %) :uneval)))
      p)))

(defn- move-to-value
  [zloc p]
  (some-> zloc
          (z/find-value p)
          (z/right)
          (move-through-meta)))

(defn- move-to-value-or-index
  "Move within a zipper. A keyword prompts descend into a map, a number moves
   to the given position. `zloc` has to point to either a seq or a vector"
  [zloc p]
  (when-let [zloc' (some-> zloc z/down)]
    (if (integer? p)
      (move-to-index zloc' p)
      (move-to-value zloc' p))))

(defn- move-path
  "Move within a zipper to the node at the given path."
  [loc path]
  (reduce move-to-value-or-index (z/up loc) path))

(defn upgrade-path
  "Upgrade the version vector at the given position."
  [loc path new-version-string]
  {:pre [(integer? (last path))]}
  (or
    (when-let [vloc (some-> (move-path loc path)
                            z/down)]
      (-> (->> (if-let [sloc (z/right vloc)]
                 (z/replace sloc new-version-string)
                 (z/insert-right vloc new-version-string))
               (z/up)
               (iterate move-up-through-meta))
          (nth (count path))
          z/down))
    loc))

(defn upgrade-artifacts
  "Upgrade the given zipper using the given artifact maps."
  [loc artifacts]
  (reduce
    (fn [loc {:keys [path latest artifact]}]
      {:pre [(map? latest)]}
      (if-let [new-version-string (:version-string latest)]
        (do
          (debugf "-- upgrading %s at %s to %s ..."
                  (pr-str (:form artifact))
                  (pr-str path)
                  (pr-str (:version-string latest)))
          (upgrade-path loc path new-version-string))
        loc))
    loc artifacts))
