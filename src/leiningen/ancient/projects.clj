(ns ^{:doc "Project Map Inspection for lein-ancient"
      :author "Yannick Scherer"}
  leiningen.ancient.projects
  (:require [leiningen.core.project :as project :only [defaults]]
            [ancient-clj.core :refer [artifact-map]]
            [ancient-clj.repository :refer [repository]]))

(defn collect-repositories
  "Get seq of repository maps from project map."
  [project]
  (prn project/defaults)
  (->>
    (:repositories project (:repositories project/defaults))
    (map second)
    (map #(if (string? %) { :url % } %))
    (filter (complement nil?))
    (map repository)))

(defn collect-artifacts
  "Take settings map created by `parse-cli` and create seq of artifact maps."
  [project settings]
  (let [deps? (:dependencies settings)
        plugins? (:plugins settings)
        dependencies (concat
                       (when deps? (:dependencies project))
                       (when plugins? (:plugins project))
                       (when-not (:no-profiles settings)
                         (concat
                           (when deps? (mapcat :dependencies (vals (:profiles project))))
                           (when plugins? (mapcat :plugins (vals (:profiles project)))))))]
    (->> (if-not (:check-clojure settings)
           (filter (complement (comp #{"org.clojure/clojure"} str first)) dependencies)
           dependencies)
      (map artifact-map)
      (distinct))))
