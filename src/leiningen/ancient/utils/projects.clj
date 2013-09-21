(ns ^{:doc "Project Map Inspection for lein-ancient"
      :author "Yannick Scherer"}
  leiningen.ancient.utils.projects
  (:require [leiningen.core.project :as project :only [defaults]]
            [leiningen.core.user :as uu :only [resolve-credentials]]
            [ancient-clj.core :refer [artifact-map]]
            [ancient-clj.repository :refer [repository]]))

;; ## Repositories

(defn collect-repositories
  "Get seq of repository maps from project map."
  [project]
  (->>
    (:repositories project (:repositories project/defaults))
    (map second)
    (map #(if (string? %) { :url % } %))
    (filter (complement nil?))
    (map uu/resolve-credentials)
    (map repository)))

;; ## Artifacts

(defn- create-artifact-map
  "Create artifact map (see ancient-clj.core/artifact-map), extended by the path
   (destined for `get-in`) to the artifact vector in the project map."
  [get-in-path artifact]
  (-> (artifact-map artifact)
    (assoc ::path get-in-path)))

(defn artifact-path
  "Get artifact path."
  [artifact-map]
  (::path artifact-map))

(defn- create-artifact-maps
  "Create artifact maps for a seq of artifact vectors."
  [get-in-path artifacts]
  (map-indexed
    (fn [i artifact]
      (create-artifact-map
        (conj (vec get-in-path) i)
        artifact))
    artifacts))

(defn- create-profile-artifact-maps
  "Create artifact maps from profile artifacts under the given key (either `:dependencies`
   or `:plugins`)."
  [project k]
  (mapcat
    (fn [[profile data]]
      (when-let [artifacts (get data k)]
        (create-artifact-maps [:profiles profile k] artifacts)))
    (:profiles project)))

(defn- create-project-artifact-maps
  "Create artifact maps from top-level project artifacts under the given key."
  [project k]
  (create-artifact-maps [k] (get project k)))

(defn- filter-clojure-maps
  "Remove Clojure artifact maps from the given seq."
  [artifact-maps]
  (filter
    (fn [{:keys [group-id artifact-id]}]
      (or (not= group-id "org.clojure")
          (not= artifact-id "clojure")))
    artifact-maps))

(defn collect-artifacts
  "Take settings map created by `parse-cli` and create seq of artifact maps."
  [project settings]
  (let [deps? (:dependencies settings)
        plugins? (:plugins settings)
        artifacts (concat
                    (when deps? (create-project-artifact-maps project :dependencies))
                    (when plugins? (create-project-artifact-maps project :plugins))
                    (when (:profiles settings true)
                      (concat
                        (when deps? (create-profile-artifact-maps project :dependencies))
                        (when plugins? (create-profile-artifact-maps project :plugins)))))]
    (if-not (:check-clojure settings)
      (filter-clojure-maps artifacts)
      artifacts)))
