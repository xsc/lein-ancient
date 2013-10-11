(ns ^{:doc "Check your Project for outdated Dependencies."
      :author "Yannick Scherer"}
  leiningen.ancient
  (:require [leiningen.ancient.check :refer [run-check-task! run-profiles-task!]]
            [leiningen.ancient.get :refer [run-get-task! run-latest-vector-task!]]
            [leiningen.ancient.upgrade :refer [run-upgrade-task! run-upgrade-profiles-task!]]
            [leiningen.core.main :as main]
            [ancient-clj.verbose :refer :all]))

(def ^:private dispatch-table
  {"get"              run-get-task!
   "profiles"         run-profiles-task!
   "upgrade"          run-upgrade-task!
   "upgrade-profiles" run-upgrade-profiles-task!
   "latest"           run-latest-vector-task!})

(defn ^:higher-order ^:no-project-needed ancient
  "Check your Projects for outdated Dependencies. 
  
   Usage:

     lein ancient [<options>] [<path>]
     lein ancient profiles [<options>]
     lein ancient get <package> [<options>]
     lein ancient upgrade [<options>] [<path>]
     lein ancient upgrade-profiles [<options>]

   Tasks:

     get                  Retrieve artifact information from Maven repositories.
     profiles             Check artifacts in '~/.lein/profiles.clj'.
     upgrade              Replace artifacts in the given file (default: './project.clj') with newer versions.
     upgrade-profiles     Replace plugins in '~/.lein/profiles.clj' with newer versions.

   Commandline Options:
  
     :aggressive          Check all available repositories (= Do not stop after first artifact match).
     :all                 Check Dependencies and Plugins.
     :allow-all           Allow SNAPSHOT and qualified versions to be reported as new.
     :allow-qualified     Allow '*-alpha*' versions & co. to be reported as new.
     :allow-snapshots     Allow '*-SNAPSHOT' versions to be reported as new.
     :check-clojure       Include Clojure (org.clojure/clojure) in checks.
     :dependencies        Check Dependencies. (default)
     :interactive         Run 'upgrade' in interactive mode, prompting whether to apply changes.
     :no-colors           Disable colorized output.
     :no-profiles         Do not check Dependencies/Plugins in Profiles.
     :no-tests            Do not run tests after upgrading a project.
     :overwrite-backup    Do not prompt if a backup file exists when upgrading a project.
     :plugins             Check Plugins.
     :print               Print result of 'upgrade' task instead of writing it to 'project.clj'.
     :recursive           Perform recursive 'check' or 'upgrade'.
  "
  [project & args]
  (let [^String t (when-let [^String t (first args)]
                    (when-not (.startsWith t ":") t))
        run-task! (get dispatch-table t)
        args (if run-task! (rest args) args)]
    (if run-task!
      (run-task! project args)
      (run-check-task! project args))))
