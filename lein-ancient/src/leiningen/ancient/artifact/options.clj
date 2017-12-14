(ns leiningen.ancient.artifact.options
  (:require [leiningen.core
             [user :as user]]
            [leiningen.ancient.verbose :refer :all]
            [ancient-clj.core :as ancient]))

(defn- format-delta
  [start]
  (->> (quot (- (System/nanoTime) start) 1000000)
       (format "\u0394 %4dms")))

(defn- wrap-single-loader
  [f repo-id]
  (fn [group id]
    (let [start (System/nanoTime)
          vs (f group id)
          tag (->> (vector
                     (format "[%s]" repo-id)
                     (format-delta start))
                   (apply format "%-9s - %-7s -"))]
      (cond (instance? Throwable vs)
            (warnf "%s failure when checking %s/%s: %s" tag group id vs)
            (empty? vs)
            (debugf "%s no results for %s/%s." tag group id)
            :else
            (debugf "%s %d versions found for %s/%s." tag (count vs) group id))
      vs)))

(defn- create-repository-loader
  [repo-id spec]
  (when spec
    (some-> (if (fn? spec)
              spec
              (-> (if (string? spec) {:url spec} spec)
                  (user/profile-auth)
                  (user/resolve-credentials)))
            (ancient/maybe-create-loader)
            (wrap-single-loader repo-id))))

(defn prepare-repositories
  "Prepate the repositories for usage with ancient-clj."
  [repositories]
  (debugf "repositories: %s" (pr-str repositories))
  (->> (for [[repo-id spec] repositories
             :let [loader (create-repository-loader repo-id spec)]]
         (if loader
           [repo-id loader]
           (warnf "[%s] could not create repository loader: %s"
                  repo-id
                  (pr-str spec))))
       (into {})))

(defn- select-mirror
  "Select a mirror for the given repository name/URL."
  [mirrors name url]
  (some
    (fn [[match mirror]]
      (when (or (and (string? match) (#{name url} match))
                (and (instance? java.util.regex.Pattern match)
                     (some #(re-matches match %) [name url])))
        mirror))
    mirrors))

(defn select-mirrors
  [repositories mirrors]
  (->> (for [[name v] repositories
             :let [url (if (string? v) v (:url v))
                   mirror (select-mirror mirrors name url)]]
         [name (or mirror v)])
       (into {})))

(defn- repository-options
  [options {:keys [repositories mirrors]}]
  (->> (-> (or repositories ancient/default-repositories)
           (select-mirrors mirrors)
           (prepare-repositories))
       (assoc options :repositories)))

(defn- version-options
  [options {:keys [snapshots? qualified?]}]
  (assoc options
         :snapshots? (boolean snapshots?)
         :qualified? (boolean qualified?)))

(defn- selector-options
  [options {:keys [dependencies? plugins? profiles? java-agents? check-clojure?
                   only exclude]
            :or {dependencies? true, profiles? true}}]
  (let [base (->> [dependencies? plugins? profiles? java-agents?
                   check-clojure?]
                  (map #(if % :include :exclude))
                  (zipmap [:dependencies :plugins :profiles :java-agents
                           :clojure])
                  (reduce
                    (fn [m [marker k]]
                      (update-in m [k] conj marker))
                    {}))]
    (cond-> base
      (seq only)    (update-in [:include] #(map (fn [x] (conj only x)) %))
      (seq exclude) (update-in [:exclude] concat exclude)
      true          (merge options))))

(defn options
  "Prepare the option map."
  ([] (options {}))
  ([opts]
   (-> {:cache (ref {})}
       (repository-options opts)
       (version-options opts)
       (selector-options opts))))
