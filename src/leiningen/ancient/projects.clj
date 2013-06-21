(ns ^{:doc "Project Map Inspection for lein-ancient"
      :author "Yannick Scherer"}
  leiningen.ancient.projects
  (:use [leiningen.ancient.version :only [version-map]]
        [leiningen.ancient.maven-metadata :only [find-retriever]])
  (:require [leiningen.core.project :as project :only [defaults]]))

(defn collect-metadata-retrievers
  "Get Retriever Functions from Project."
  [project]
  (->>
    (:repositories project (:repositories project/defaults))
    (map second)
    (map find-retriever)))

(defn- dependency-map
  "Create dependency map (:group-id, :artifact-id, :version)."
  [[dep version & _]]
  (let [dep (str dep)
        [g a] (if (.contains dep "/")
                (.split dep "/" 2)
                [dep dep])] 
    (-> {}
      (assoc :group-id g)
      (assoc :artifact-id a)
      (assoc :version (version-map version)))))

(defn collect-dependencies
  "Take settings map created by `parse-cli` and create seq of dependency vectors."
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
      (map dependency-map)
      (distinct))))
