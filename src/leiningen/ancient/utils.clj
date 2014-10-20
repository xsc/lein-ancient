(ns leiningen.ancient.utils
  (:require [leiningen.ancient.artifact.options :as o]
            [leiningen.ancient
             [cli :as cli]
             [collect :as collect]
             [verbose :refer :all]]
            [jansi-clj.core :as color]))

;; ## Reporting

(defn throwable?
  "Check whether the given value is a Throwable."
  [v]
  (instance? Throwable v))

(defn path-string
  "Create a string representing the path"
  [{:keys [path]}]
  (let [p (.getPath ^java.io.File path)
        h (System/getProperty "user.home")
        l (System/getenv "LEIN_HOME")]
    (if (.startsWith p h)
      (str "~" (subs p (count h)))
      p)))

(defmacro with-success
  "Bind the given form to the given symbol but execute the given body
   only if no Throwable is thrown or returned."
  [[sym form] & body]
  `(try
     (let [r# ~form]
       (if (throwable? r#)
         r#
         (let [~sym r#]
           ~@body)))
     (catch Throwable t# t#)))

(defmacro call-with-throwables
  "Run the given body but log all Throwables."
  [& body]
  `(let [v# (with-success [r# (do ~@body)] r#)]
     (when (throwable? v#)
       (errorf "%s" (pr-str v#)))
     v#))

(defn call-file
  [f o file]
  (call-with-throwables
    (f o file)))

(defn report-file-failures
  [{:keys [opts]} sq]
  (if (:report? opts)
    (let [sq (sort-by first sq)]
      (verbosef "Report:%n-------")
      (doseq [[file t] sq]
      (verbosef "%s %s"
                (if (throwable? t)
                  (color/red "[fail]")
                  (color/green "[ok]  "))
                file)))
    (dorun sq)))

(defn call-files
  "Call the given function on all `DependencyFile` values, logging
   the filename and potential errors."
  [f o files]
  (->> (for [file files
             :let [ps (path-string file)]]
         (do
           (verbosef "-- %s" ps)
           (let [r (call-file f o file)]
             (verbosef "")
             [ps r])))
       (report-file-failures o)))

;; ## Options

(defn parse
  "Create a map of `:artifact-opts`, `:opts` and `:args` from the given
   project and arguments."
  [{:keys [repositories] :as project} args & {:keys [change-defaults exclude fixed]}]
  (try
    (let [[opts' rst] (cli/parse args
                                 :change-defaults change-defaults
                                 :exclude exclude)
          opts (merge opts' fixed)
          artifact-opts (-> (assoc opts :repositories repositories)
                            (o/options))]
      {:artifact-opts artifact-opts
       :opts opts
       :project project
       :args rst})
    (catch Throwable t
      (errorf "%s" (.getMessage t)))))

;; ## Files

(defn call-on-project-files
  "Call the given function on all files found based on the given project
   map, options and arguments. Will not print file paths if only the current
   project's file is processed."
  [f {:keys [opts args project] :as o}]
  (if-let [files-if-multiple (if (:recursive? opts)
                               (if (seq args)
                                 (mapcat collect/recursive-project-files args)
                                 (collect/recursive-project-files "."))
                               (if (seq args)
                                 (keep collect/project-file-at args)))]
    (call-files f o files-if-multiple)
    (if (:root project)
      (->> (collect/current-project-file project)
           (call-file f o))
      (warnf "not inside of a project."))))

(defn call-on-profiles-files
  "Call the given function on all profiles files found based on the given project
   map, options and arguments. Will always print the files paths."
  [f {:keys [opts args project] :as o}]
  (->> (if (seq args)
         (keep collect/profiles-file-at args)
         (collect/profiles-files project))
       (call-files f o)))

(defmacro with-temp-file
  "Create file with the given contents, then run the body."
  [[sym data] & body]
  `(let [f# (java.io.File/createTempFile "ancient" ".clj")]
     (try
       (spit f# ~data)
       (let [~sym f#]
         ~@body)
       (finally
         (.delete f#)))))

;; ## Task Function

(defmacro deftask
  [sym {:keys [docstring exclude fixed]} [opts] & body]
  `(let [ex# ~exclude
         fx# ~fixed]
     (-> (defn ~sym
           [project# & args#]
           (when-let [opts# (parse project# args#
                                   :exclude ex#
                                   :fixed fx#)]
             (let [cli# (:opts opts#)]
               (debugf "cli options: %s" (pr-str cli#))
               (if (:colors? cli#)
                 (color/enable!)
                 (color/disable!)))
             (let [~opts opts#]
               ~@body)))
         (cli/doc! ~docstring :exclude ex#))))
