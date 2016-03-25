(ns ancient-clj.core
  (:require [ancient-clj
             [artifact :as artifact]
             [io :as io]
             [load :refer [load-versions!]]]
            [version-clj.core :as v]
            [potemkin :refer [import-vars]]))

(import-vars
  [ancient-clj.artifact
   read-artifact])

;; ## Repositories

(def default-repositories
  {"central" "http://repo1.maven.org/maven2"
   "clojars" "https://clojars.org/repo"})

;; ## Loaders

(defn create-loaders
  "Create loader map for a seq of ID/settings pairs representing
   different repositories.

   `wrap` will be called on each loader function."
  [m & {:keys [wrap] :or {wrap identity}}]
  (->> (for [[id v] m]
         [id (wrap (io/loader-for v))])
       (into {})))

(defn ^{:added "0.3.12"} maybe-create-loader
  "Create loader function for the given spec. Will return `nil` for
   unknown loader types."
  [spec]
  (io/maybe-loader-for spec))

(defn ^{:added "0.3.12"} maybe-create-loaders
  "Create loader map for a seq of ID/settings pairs representing
   different repositories. Will contain `nil` values for unknown
   loaders.

   `wrap` will be called on each loader function."
  [m & {:keys [wrap] :or {wrap identity}}]
  (->> (for [[id v] m]
         [id (some-> v io/maybe-loader-for wrap)])
       (into {})))

;; ## Result Handling

(defn- versions->maps
  "Convert the sequence of version strings to maps of `:version-string` and
   `:version` (a version-clj seq)."
  [v]
  (->> v
       (map (juxt identity v/version->seq))
       (map #(zipmap [:version-string :version] %))))

(defn- remove-snapshots
  "Remove all SNAPSHOT versions from the version seq."
  [versions]
  (remove (comp v/snapshot? :version) versions))

(defn- remove-qualified
  "Remove all qualified (and not SNAPSHOT) versions from the version seq."
  [versions]
  (remove
    (fn [{:keys [version]}]
      (and (not (v/snapshot? version))
           (v/qualified? version)))
    versions))

(defn- sort-versions
  "Sort the given versions in descending order."
  [versions]
  (sort
    (fn [a b]
      (v/version-seq-compare
        (:version b)
        (:version a)))
    versions))

(defn- process-versions
  "Process versions based on the given options."
  [{:keys [snapshots? qualified? sort]
    :or {sort       :desc
         snapshots? true
         qualified? true}}
   versions]
  (cond->> (distinct versions)
    (not snapshots?)  remove-snapshots
    (not qualified?)  remove-qualified
    (not= sort :none) sort-versions
    (= sort :asc)     reverse))

;; ## Core Functionality

(defn versions-per-repository!
  "Return a map with either a seq of versions or a Throwable for each
   repository.

   - `:repositories`: a map of repository ID -> loader specification.
   - `:snapshots?`: if set to false, SNAPSHOT versions will be filtered.
   - `:qualified?`: if set to false, qualified versions (e.g. \"*-alpha\") will be filtered.
   - `:sort`: the order of the resulting versions seqs (`:desc`, `:asc`, `:none`).

   A loader specification can either be a map (with at least the key `:uri`), a string
   or a two-parameter function (taking artifact group and ID).

   Versions will be given as maps of `:version-string` and `:version` where the latter
   represents a version-clj value."
  [artifact & [{:keys [repositories snapshots? qualified? sort]
                :or {repositories default-repositories
                     snapshots?   true
                     qualified?   true
                     sort         :desc}
                :as opts}]]
  (let [loaders (create-loaders repositories)
        artifact' (artifact/read-artifact artifact)]
    (->> (for [[id vs] (load-versions! loaders artifact')]
           (if (sequential? vs)
             (if (seq vs)
               (->> (versions->maps vs)
                    (process-versions opts)
                    (vector id)))
             [id vs]))
         (into {}))))

(defn versions!
  "Return a seq of version maps.
   (For possible options see `ancient-clj.core/versions-per-repository!`.)"
  [artifact & [{:keys [repositories snapshots? qualified? sort]
                :or {repositories default-repositories
                     snapshots?   true
                     qualified?   true
                     sort         :desc}
                :as opts}]]
  (let [loaders (create-loaders repositories)
        artifact' (artifact/read-artifact artifact)]
    (->> (for [[id vs] (load-versions! loaders artifact')
               :when (sequential? vs)]
           (versions->maps vs))
         (apply concat)
         (process-versions opts))))

(defn version-strings!
  "Return a seq of version strings.
   (For possible options see `ancient-clj.core/versions-per-repository!`.)"
  [artifact & [opts]]
  (->> (versions! artifact opts)
       (map :version-string)))

(defn latest-version!
  "Return a map representing the latest artifact version.
   (For possible options see `ancient-clj.core/versions-per-repository!`.)"
  [artifact & [opts]]
  (->> (assoc opts :sort :desc)
       (versions! artifact)
       (first)))

(defn latest-version-string!
  "Return a string representing the latest artifact version.
   (For possible options see `ancient-clj.core/versions-per-repository!`.)"
  [artifact & [opts]]
  (:version-string
    (latest-version! artifact opts)))

(defn artifact-outdated?
  "Check whether the given artifact is outdated, returning either `nil` or
   a map representing the latest version.
   (For possible options see `ancient-clj.core/versions-per-repository!`.)"
  [artifact & [opts]]
  (let [artifact'(read-artifact artifact)]
    (if-let [latest (latest-version! artifact' opts)]
      (if (neg? (v/version-seq-compare
                  (:version artifact')
                  (:version latest)))
        latest))))

(defn artifact-outdated-string?
  "Check whether the given artifact is outdated, returning either `nil` or
   a string representing the latest version.
   (For possible options see `ancient-clj.core/versions-per-repository!`.)"
  [artifact & [opts]]
  (:version-string
    (artifact-outdated? artifact opts)))
