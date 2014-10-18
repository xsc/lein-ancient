(ns leiningen.ancient
  (:require [leiningen.ancient
             [check :as c]
             [get :as g]
             [upgrade :as u]
             [verbose :refer :all]]
            [leiningen.core.main :as main]
            [jansi-clj.auto]))

(defn
  ^:higher-order ^:no-project-needed
  ^{:subtasks [#'c/check #'c/profiles
               #'g/get #'g/latest
               #'u/upgrade #'u/upgrade-profiles]}
  ancient
  "Check your projects and profiles for outdated dependencies/plugins."
  [project & args]
  (let [run #(apply % project (rest args))]
    (case (first args)
      "check"            (run c/check)
      "get"              (run g/get)
      "latest"           (run g/latest)
      "profiles"         (run c/profiles)
      "upgrade"          (run u/upgrade)
      "upgrade-profiles" (run u/upgrade-profiles)
      (apply c/check project args))))
