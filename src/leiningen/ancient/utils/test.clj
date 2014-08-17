(ns ^{ :doc "Regression Testing for lein-ancient"
       :author "Yannick Scherer" }
  leiningen.ancient.utils.test
  (:require [leiningen.core.main :as main]))

(defn- run-test-task!
  "Run a single test task, suppressing output and returnin a boolean value indicating
   whether the task finished without calling `main/abort`."
  [project task]
  (try
    (let [[task-name & task-args] task]
      (main/info "Running Test Task" (pr-str task) "...")
      (binding [main/*exit-process?* false]
        (main/apply-task task-name project task-args)
        true))
    (catch Exception ex false)))

(defn run-tests!
  "Run regression tests (using the alias \"test-ancient\" in the project map or \"test\")."
  [project]
  (let [task (or (get-in project [:aliases "test-ancient"])
                 (get-in project [:aliases "test"])
                 "test")
        task (if (sequential? task) task [task])
        r (run-test-task! project task)]
    (when-not r
      (main/info "Tests failed (use ':no-tests' if you want to suppress testing)."))
    r))
