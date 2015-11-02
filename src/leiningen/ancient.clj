(ns leiningen.ancient
  (:require [leiningen.ancient
             [check :as c]
             [get :as g]
             [upgrade :as u]
             [verbose :refer :all]]
            [leiningen.core.main :as main]
            [jansi-clj.auto]))


(defn- as-deprecated
  [given-task new-task f arg]
  (main/warn "WARN: the subtask"
             (str "'" given-task "'")
             "is deprecated, use"
             (str "'" new-task "'")
             "instead.\n")
  (f arg))

(defn
  ^:higher-order ^:no-project-needed
  ^{:subtasks [#'c/check
               #'c/check-profiles
               #'g/show-versions
               #'g/show-latest
               #'u/upgrade
               #'u/upgrade-profiles]}
  ancient
  "Check your projects and profiles for outdated dependencies/plugins."
  [project & args]
  (let [run #(apply % project (rest args))
        run-deprecated #(as-deprecated (first args) %1 run %2)]
    (case (first args)
      "check"                       (run c/check)
      "check-profiles"              (run c/check-profiles)
      "get"                         (run-deprecated "show-versions" g/show-versions)
      "profiles"                    (run-deprecated "check-profiles" c/check-profiles)
      "show-versions"               (run g/show-versions)
      ("show-latest" "latest")      (run g/show-latest)
      "upgrade"                     (run u/upgrade)
      "upgrade-profiles"            (run u/upgrade-profiles)
      (apply c/check project args))))
