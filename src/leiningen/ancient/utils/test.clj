(ns ^{ :doc "Regression Testing for lein-ancient"
       :author "Yannick Scherer" }
  leiningen.ancient.utils.test
  (:require [leiningen.core.main :as main]
            [testem.core :as testem :only [create-test-tasks]]
            [ancient-clj.verbose :refer :all]))

(defn run-tests!
  "Run regression tests (using lein-testem) on the given project map."
  [project]
  (let [tasks (or
                (when-let [t (get-in project [:aliases "test-ancient"])]
                  [[:user-specified {:test t}]])
                (testem/create-test-tasks project))]
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
