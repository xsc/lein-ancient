(ns ^{:doc "Check your Project for outdated Dependencies."
      :author "Yannick Scherer"}
  leiningen.ancient
  (:require [leiningen.core.project :as project :only [defaults]]
            [clojure.data.xml :as xml :only [parse-str]]
            [colorize.core :as colorize]))

;; ## Utility Functions

(defn- id->path
  "Convert ID to URL path by replacing dots with slashes."
  [^String s]
  (if-not s "" (.replace s "." "/")))

;; ## Colorize

(def ^:dynamic ^:private *colors* true)

(defmacro ^:private conditional-colors
  [& ids]
  `(do
     ~@(for [id ids]
         `(defn ~(vary-meta (symbol (name id)) assoc :private true)
            [& msg#]
            (let [msg# (apply str msg#)]
              (if *colors*
                (~id msg#)
                msg#))))))
(conditional-colors colorize/yellow colorize/green colorize/red)

;; ## Verbose

(def ^:dynamic ^:private *verbose* nil)

(defn- verbose
  "Write Log Message."
  [& msg]
  (when *verbose*
    (binding [*out* *err*]
      (println "(verbose)" (apply str msg)))))

;; ## Version Comparison

(def ^:private VERSION_REGEX 
  #"^([0-9]+)\.([0-9]+)(\.([0-9]+))?(\-(.+))?$")

(defn version-map
  "Create version map (:major, :minor, :incremental, :qualifier) from 
   version string. Conforms to Maven's version string format."
  [^String version]
  (->
    (if-let [[_ major minor _ incremental _ qualifier] (re-find (re-matcher VERSION_REGEX version))]
      {:major (Integer/parseInt major) 
       :minor (Integer/parseInt minor)
       :incremental (if incremental (Integer/parseInt incremental) 0)
       :qualifier (if qualifier (.toLowerCase ^String qualifier) qualifier)}
      {:major -1
       :minor -1
       :incremental -1
       :qualifier version })
    (assoc :version-str version)))

(defn- qualifier-compare
  "Compare two qualifier strings. This tries to introduce numeric comparison
   when using qualifiers like 'alpha5' and 'alpha12'"
  [q0 q1]
  (cond (= q0 q1)          0
        (not q0)           1
        (not q1)          -1
        (= q0 "snapshot")  1
        (= q1 "snapshot") -1
        :else (or 
                (when-let [[_ qa ra] (re-find (re-matcher #"^([a-zA-Z_-]*)([0-9]+)$" q0))]
                  (when-let [[_ qb rb] (re-find (re-matcher #"^([a-zA-Z_-]*)([0-9]+)$" q1))]
                    (when (= qa qb)
                      (if (< (Integer/parseInt ra) (Integer/parseInt rb))
                        -1
                        1))))
                (.compareTo q0 q1))))

(defn- version-map-compare
  "Compare two version maps."
  [m0 m1]
  (cond (< (:major m0) (:major m1))             -1
        (< (:major m1) (:major m0))              1
        (< (:minor m0) (:minor m1))             -1
        (< (:minor m1) (:minor m0))              1
        (< (:incremental m0) (:incremental m1)) -1
        (< (:incremental m1) (:incremental m0))  1
        :else (qualifier-compare (:qualifier m0) (:qualifier m1))))

(defn version-outdated?
  "Check if the first version is outdated."
  [v0 v1]
  (= -1 (version-map-compare v0 v1)))

(defn version-compare
  "Compare two version strings."
  [v0 v1]
  (version-map-compare (version-map v0) (version-map v1)))

;; ## Metadata Retrieval & Analysis

(defn build-metadata-url
  "Get URL to maven-metadata.xml of the given package."
  [^String repository-url ^String group-id ^String artifact-id]
  (str repository-url 
       (if (.endsWith repository-url "/") "" "/")
       (id->path group-id) "/" artifact-id
       "/maven-metadata.xml"))

(defn retrieve-metadata!
  "Find metadata XML file in one of the given Maven repositories."
  [repository-urls group-id artifact-id]
  (loop [urls repository-urls]
    (when (seq urls)
      (let [u (build-metadata-url (first urls) group-id artifact-id)]
        (if-let [data (try (slurp u) (catch Exception _ nil))]
          data
          (recur (rest urls)))))))

(defn version-seq
  "Get all the available versions from the given metadata XML string."
  [mta]
  (for [t (xml-seq (xml/parse-str mta))
        :when (= (:tag t) :version)]
    (first (:content t))))

(defn snapshot?
  "Check if the given version is a SNAPSHOT."
  [v]
  (= (:qualifier v) "snapshot"))

(defn- filter-versions
  "Remove all versions that do not fit the given settings map."
  [{:keys [allow-snapshots allow-qualified]} version-maps]
  (let [v version-maps
        v (if-not allow-snapshots (filter (complement snapshot?) v) v)
        v (if-not allow-qualified (filter (comp nil? :qualifier) v) v)]
    v))

(defn latest-version
  "Get map of the latest available version in the given metadata XML
   string."
  ([mta] (latest-version mta nil))
  ([mta settings]
   (->> (version-seq mta)
     (map version-map)
     (filter-versions settings)
     (sort version-map-compare)
     (last))))

;; ## Project Map Inspection

(defn- get-repository-urls
  "Get Repository URLs from Project."
  [project]
  (->>
    (:repositories project (:repositories project/defaults))
    (map second)
    (map :url)))

(defn dependency-map
  "Create dependency map (:group-id, :artifact-id, :version)."
  [[dep version & _]]
  (let [dep (str dep)
        [g a] (if (.contains dep "/")
                (.split dep "/" 2)
                [dep dep])] 
    (-> {}
      (assoc :dependency-str dep)
      (assoc :group-id g)
      (assoc :artifact-id a)
      (assoc :version (version-map version)))))

;; ## Actual Check Logic

(defn- version-string
  [version]
  (str "\"" (:version-str version) "\""))

(defn- artifact-string
  [group-id artifact-id version]
  (let [f (if (= group-id artifact-id)
            artifact-id
            (str group-id "/" artifact-id))]
    (str "[" f " " (green (version-string version)) "]")))

(defn- check-packages
  "Check the packages found at the given key in the project map.
   Will check the given repository urls for metadata."
  [repos packages settings]
  (let [retrieve! (partial retrieve-metadata! repos)]
    (doseq [{:keys [group-id artifact-id version] :as dep} 
            (map dependency-map packages)]
      (verbose "Checking " group-id "/" artifact-id " (current version: " (version-string version) ") ...")
      (if-let [mta (retrieve! group-id artifact-id)]
        (if-let [latest (latest-version mta settings)]
          (when (version-outdated? version latest)
            (println
              (artifact-string group-id artifact-id latest)
              "is available but we use"
              (yellow (version-string version))))
          (verbose "No latest Version found!"))
        (verbose "No Metadata File found!")))))

;; ## CLI

(def ^:private CLI_FLAGS
  "Available CLI Flags."
  #{":dependencies" ":all" ":plugins" ":allow-snapshots"
    ":allow-qualified" ":no-profiles" ":check-clojure"
    ":verbose" ":no-colors"})

(defn parse-cli
  "Parse Command Line, return map of Settings."
  [args]
  (let [data (->> (for [^String flag args]
                    (when (contains? CLI_FLAGS flag)
                      (vector
                        (keyword (.substring flag 1))
                        true)))
               (into {}))]
    (cond (:all data) (assoc data :dependencies true :plugins true) 
          (:plugins data) data
          :else (assoc data :dependencies true))))

(defn collect-dependencies
  "Take settings map created by `parse-cli` and create seq of dependency vectors."
  [project settings]
  (let [deps? (:dependencies settings)
        plugins? (:plugins settings)
        dependencies (distinct
                       (concat
                         (when deps? (:dependencies project))
                         (when plugins? (:plugins project))
                         (when-not (:no-profiles settings)
                           (concat
                             (when deps? (mapcat :dependencies (vals (:profiles project))))
                             (when plugins? (mapcat :plugins (vals (:profiles project))))))))]
    (if-not (:check-clojure settings)
      (filter (complement (comp #{"org.clojure/clojure"} str first)) dependencies)
      dependencies)))

(defn ^:no-project-needed ancient
  "Check your Projects for outdated Dependencies. 
   
   Commandline Options:
  
     :all                 Check Dependencies and Plugins.
     :dependencies        Check Dependencies. (default)
     :plugins             Check Plugins.
     :no-profiles         Do not check Dependencies/Plugins in Profiles.
     :allow-qualified     Allow '*-alpha*' versions & co. to be reported as new.
     :allow-snapshots     Allow '*-SNAPSHOT' versions to be reported as new.
     :check-clojure       Include Clojure (org.clojure/clojure) in checks.
     :verbose             Produce progress indicating messages.
     :no-colors           Disable colorized output.
  "
  [project & args]
  (let [settings (parse-cli args)]
    (binding [*verbose* (:verbose settings)
              *colors* (not (:no-colors settings))]
      (let [repos (get-repository-urls project)
            deps (collect-dependencies project settings)]
        (verbose "Checking " (count deps) " Dependencies ...")
        (check-packages repos deps settings)
        (verbose "Done.")))))
