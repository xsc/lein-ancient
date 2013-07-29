(ns ^{ :doc "Rewrite project.clj to include latest versions of dependencies." 
       :author "Yannick Scherer" }
  leiningen.ancient.tasks.update-project
  (:require [leiningen.ancient.projects :refer [collect-repositories]]
            [leiningen.ancient.cli :refer [parse-cli]]
            [ancient-clj.verbose :refer :all]
            [ancient-clj.core :as anc]
            [rewrite-clj.parser :as p]
            [rewrite-clj.zip :as z]
            [rewrite-clj.printer :as prn]))

(defn read-project
  [f]
  (z/edn (p/parse-file f)))

(defn write-project
  [zloc]
  (-> zloc z/root prn/print-edn))

(defn map-get
  [loc ks]
  (reduce
    (fn [loc k]
      (-> loc z/down (z/find-value k) z/right))
    loc ks))

(defn upgrade-dependencies
  [deps]
  (->
    (loop [loc (z/down deps)]
      (if-not (= (z/tag loc) :vector)
        loc
        (let [[artifact version] (z/sexpr loc)]
          (prn artifact version)
          (if-let [r (z/right loc)] 
            (recur r)
            loc))
        )
      )
    z/up))

(defn upgrade-profiles
  [profiles]
  (->
    (loop [loc (z/down profiles)]
      (if-not (keyword? (z/sexpr loc))
        loc
        (let [deps (-> loc z/right (map-get [:dependencies]))
              deps (-> deps upgrade-dependencies z/up)]
          (if-let [r (z/right deps)]
            (recur r)
            deps))))
    z/up))

(defn upgrade-project
  [proj]
  (let [deps (-> proj (z/find-value z/next 'defproject) (z/find-value :dependencies) z/right)
        deps (upgrade-dependencies deps)
        profiles (-> deps z/leftmost (z/find-value :profiles) z/right)
        profiles (upgrade-profiles profiles)]
    (-> profiles z/root prn/print-edn)))
