(ns leiningen.ancient.artifact.files
  (:require [leiningen.ancient.artifact
             [check :as check]
             [reader :as reader]
             [zip :as z]]
            [leiningen.ancient.verbose :refer :all]
            [pandect.algo.adler32 :refer [adler32-file]]
            [potemkin :refer [defprotocol+]]
            [clojure.java.io :as io]))

;; ## Protocol

(defprotocol+ Dependencies
  "Protocol for files representing dependencies."
  (read! [this]
    "Read the file data, initialize the dependency file value.")
  (check! [this options]
    "Check the dependencies for newer versions. Returns a seq of maps
     with `:path`, `:latest` and `:artifact` keys, or a Throwable")
  (upgrade! [this outdated]
    "Upgrade the given outdated dependencies (produced by `check!`).
     Returns either an upgraded `Dependencies` value or a `Throwable`.")
  (write! [this writer]
    "Write the upgraded data to the given writer.")
  (write-out! [this]
    "Write the upgraded data back to the original source."))

;; ## Helpers

(defmacro with-throwables
  [& body]
  `(try
     (do ~@body)
     (catch Throwable t#
       t#)))

(defn- drop-prefixes
  "Drop the given prefix from all artifact paths."
  [prefix artifacts]
  (let [c (count prefix)]
    (map
      (fn [artifact]
        (update-in artifact [:path] #(vec (drop c %))))
      artifacts)))

(defn- generate-checksum
  "Generate checksum for file."
  [path]
  (adler32-file path))

(defn- assert-checksum
  "Assert that the given file still has the given checksum."
  [path checksum]
  (when (not= (adler32-file path) checksum)
    (throw (Exception. "checksum of file changed before attempting upgrade."))))

;; ## File

(defrecord DependencyFile [read-fn zipper-fn check-post-fn
                           path checksum data zipper]
  Dependencies
  (read! [this]
    (with-throwables
      (->> {:checksum (generate-checksum path)
            :data     (read-fn path)
            :zipper   (zipper-fn path)}
           (merge this))))
  (check! [this options]
    {:pre [(map? data)]}
    (with-throwables
      (->> (check/collect-and-check-artifacts! options data)
           ((or check-post-fn identity)))))
  (upgrade! [this outdated]
    {:pre [zipper]}
    (with-throwables
      (update-in this [:zipper] z/upgrade-artifacts outdated)))
  (write! [this writer]
    {:pre [zipper]}
    (with-throwables
      (z/write-zipper! zipper writer)))
  (write-out! [this]
    (with-throwables
      (assert-checksum path checksum)
      (with-open [w (io/writer path)]
        (write! this w)))))

(defn dependency-file
  [path & {:keys [read-fn zipper-fn check-post-fn] :as logic}]
  (let [f (io/file path)]
    (map->DependencyFile
      (assoc logic :path f))))

;; ## File Types

(defn project-file
  "Create new `DependencyFile` value based on project file."
  ([]
   (project-file "project.clj"))
  ([path]
   (dependency-file
     path
     :read-fn   reader/read-project-map!
     :zipper-fn z/read-project-zipper!)))

(defn profiles-file
  "Create new `DependencyFile` value based on project file."
  ([path]
   (profiles-file path [:profiles]))
  ([path prefix]
   (dependency-file
     path
     :read-fn       #(reader/read-profiles-map! % prefix)
     :zipper-fn     z/read-profiles-zipper!
     :check-post-fn #(drop-prefixes prefix %))))

;; ## Example

(comment
  (do
    (require '[leiningen.ancient.artifact.options :as o])
    (defonce opts (o/options {:plugins? true}))
    (def p
      (->> (io/file (System/getProperty "user.home") ".lein" "profiles.clj")
           (profiles-file)
           (read!)))
    (let [outdated (check! p opts)]
      (-> (upgrade! p outdated)
          (write! *out*)))))
