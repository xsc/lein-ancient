(ns ^{ :doc "Regression Testing for lein-ancient"
       :author "Yannick Scherer" }
  leiningen.ancient.tasks.test
  (:require [leiningen.with-profile :as prf]
            [leiningen.core.project :as prj]
            [leiningen.core.main :as main]
            [leiningen.ancient.cli :refer [parse-cli]]
            [ancient-clj.verbose :refer :all]))

;; ## Helpers

(defn- dependencies->map
  "Convert dependency vectors into a map of `artifact -> version` pairs."
  [artifact-vectors]
  (->> artifact-vectors
    (map (juxt first second))
    (into {})))

(defn- detect-clojure-versions
  "Return names of profiles that contain a `org.clojure/clojure` dependency."
  [project]
  (let [user-deps (dependencies->map (:dependencies project))
        dev-version (get user-deps 'org.clojure/clojure)]
    (keep
      (fn [[profile m]]
        (let [deps (dependencies->map (:dependencies m))]
          (when-let [v (get deps 'org.clojure/clojure)]
            (when (not= v dev-version)
              (name profile)))))
      (:profiles project))))

(defn- generate-framework-call
  "Checks project for the given artifact and creates a vector of
   `[task profiles]` if it was found."
  [project task k artifact]
  (let [user-deps (dependencies->map (get project k))]
    (if (contains? user-deps artifact)
      (vector task "")
      (first
        (keep
          (fn [[profile m]]
            (let [deps (dependencies->map (get m k))]
              (when (contains? deps artifact)
                (vector task (when-not (= profile :dev)
                               [(name profile)])))))
          (:profiles project))))))

(defn- generate-test-call
  "Generate test call to use (as `[task profiles]` pair). Supports `midje` and
   `clojure.test`."
  [project]
  (or
    (generate-framework-call project "midje" :plugins 'lein-midje)
    ["test" nil]))

(defn- generate-compound-test-call
  "Based on a test call, create a compound call that tests against the available Clojure
   versions."
  [project [task profiles]]
  (let [profiles (vec (cons "dev" profiles))
        clojure-profiles (cons nil (detect-clojure-versions project))]
    (->> clojure-profiles
      (map #(conj profiles %))
      (map #(clojure.string/join "," %))
      (clojure.string/join ":")
      (vector task))))

;; ## Run Tests

(defn- run-single-test!
  "Run a single test, given as a `[task profiles]` pair."
  [project [task profiles]]
  (try
    (binding [main/*exit-process?* false]
      (verbose "Running 'lein " task " with-profile " profiles "' ...")
      (prf/with-profile project profiles task)
      (verbose "Tests passed.")
      true)
    (catch Exception ex
      (verbose "Tests failed.")
      (when-not (.startsWith (.getMessage ex) "Suppressed")
        (println (red "Exception running") "lein" task "with-profile" profiles)
        (println (.getMessage ex)))
      false)))

(defn run-all-tests!
  "Run tests based on auto-detection on project."
  [project]
  (let [c (generate-test-call project)
        c (generate-compound-test-call project c)]
    (run-single-test! project c)))

;; ## Run with refreshed Project File

(defn run-all-tests-with-refresh!
  "Run regression tests (without knowing what test mechanism(s) to use) on the given project map."
  [project]
  (if-not (:root project)
    true
    (let [project (prj/read (str (:root project) "/project.clj"))]
      (run-all-tests! project))))

;; ## Task

(defn run-test-task!
  "Run auto-detected test framework."
  [project args]
  (let [settings (parse-cli args)]
    (with-settings settings
      (when-not (run-all-tests! project)
        (main/abort)))))
