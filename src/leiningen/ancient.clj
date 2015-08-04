(ns leiningen.ancient
  (:require [leiningen.ancient
             [check :as c]
             [get :as g]
             [upgrade :as u]
             [verbose :refer :all]]
            [jansi-clj.auto]))

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
  (let [run #(apply % project (rest args))]
    (case (first args)
      "check"                       (run c/check)
      ("check-profiles" "profiles") (run c/check-profiles)
      ("show-versions" "get")       (run g/show-versions)
      ("show-latest" "latest")      (run g/show-latest)
      "upgrade"                     (run u/upgrade)
      "upgrade-profiles"            (run u/upgrade-profiles)
      (apply c/check project args))))
