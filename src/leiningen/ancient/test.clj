(ns leiningen.ancient.test
  (:require [leiningen.core
             [main :as main]
             [project :as prj]]
            [leiningen.ancient.artifact.files :as f]
            [leiningen.ancient
             [verbose :refer :all]]))

(defn- run-test-task!
  "Run test task. Will throw or return true."
  [project task]
  (try
    (let [[task-name & task-args] task
          project' (prj/init-project project)]
      (verbosef "%nrunning test task %s ..." (pr-str task))
      (binding [main/*exit-process?* false]
        (main/apply-task task-name project' task-args)
        true))
    (catch Throwable t
      t)))

(defn run-tests!
  "Run tests contained in the alias 'test-ancient' or 'test', with
   the standard 'test' task being the default."
  [project]
  (let [task (or (get-in project [:aliases "test-ancient"])
                 (get-in project [:aliases "test"])
                 "test")
        task (if (sequential? task) task [task])]
    (run-test-task! project task)))
