(ns ^{ :doc "Regression Testing for lein-ancient"
       :author "Yannick Scherer" }
  leiningen.ancient.tasks.test
  (:require [leiningen.ancient.cli :refer [parse-cli]]
            [leiningen.core.project :as prj]
            [leiningen.core.main :as main]
            [testem.core :as testem :only [create-test-tasks]]
            [ancient-clj.verbose :refer :all]))

;; ## Run Tests

(defn run-tests!
  "Run regression tests (using lein-testem) on the given project map."
  [project]
  (let [tasks (testem/create-test-tasks project)]
    (try
      (binding [main/*exit-process?* false]
        (doseq [[framework {:keys [test]}] tasks]
          (let [[task-name & task-args] test] 
            (main/info "Running" (str "[" framework "]") "Tests ...")
            (main/debug "Test Call:" (pr-str test))
            (binding [main/*debug* false
                      main/*info* false]
              (main/apply-task task-name project task-args))))
        true)
      (catch Exception ex 
        (main/info "Tests failed (use ':no-tests' if you want to surpress testing).")
        false))))

(defn run-tests-with-refresh!
  "Run regression tests (using lein-testem) after reloading the given project map."
  [project]
  (if-not (:root project)
    true
    (let [project (prj/read (str (:root project) "/project.clj"))]
      (run-tests! project))))

;; ## Task

(defn run-test-task!
  "Run auto-detected test framework."
  [project args]
  (let [settings (parse-cli args)]
    (with-settings settings
      (when-not (run-tests! project)
        (main/abort)))))
