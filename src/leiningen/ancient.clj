(ns ^{:doc "Check your Project for outdated Dependencies."
      :author "Yannick Scherer"}
  leiningen.ancient
  (:require [leiningen.ancient.check :refer [run-check-task! run-file-check-task!]]
            [leiningen.ancient.get :refer [run-get-task!]]
            [leiningen.ancient.upgrade :refer [run-upgrade-task! run-upgrade-global-task!]]
            [leiningen.core.main :as main]
            [ancient-clj.verbose :refer :all]))

(def ^:private dispatch-table
  {"get"            run-get-task!
   "upgrade"        run-upgrade-task!
   "upgrade-global" run-upgrade-global-task!
   "check"          run-file-check-task!
   nil              run-check-task!})

(defn ^:higher-order ^:no-project-needed ancient
  "Check your Projects for outdated Dependencies. 
  
   Usage:

     lein ancient [<options>]
     lein ancient check <project-file> [<option>]
     lein ancient get <package> [<options>]
     lein ancient upgrade [<options>]
     lein ancient upgrade-global [<options>]

   Tasks:

     check                Check a given project file.
     get                  Retrieve artifact information from Maven repositories.
     upgrade              Replace artifacts in your 'project.clj' with newer versions.
     upgrade-global       Replace plugins in '~/.lein/profiles.clj' with newer versions.

   Commandline Options:
  
     :aggressive          Check all available repositories (= Do not stop after first artifact match).
     :all                 Check Dependencies and Plugins.
     :allow-qualified     Allow '*-alpha*' versions & co. to be reported as new.
     :allow-snapshots     Allow '*-SNAPSHOT' versions to be reported as new.
     :allow-all           Allow SNAPSHOT and qualified versions to be reported as new.
     :check-clojure       Include Clojure (org.clojure/clojure) in checks.
     :dependencies        Check Dependencies. (default)
     :interactive         Run 'upgrade' in interactive mode, prompting whether to apply changes.
     :no-colors           Disable colorized output.
     :no-profiles         Do not check Dependencies/Plugins in Profiles.
     :no-tests            Do not run tests after upgrading a project.
     :overwrite-backup    Do not prompt if a backup file exists when upgrading a project.
     :plugins             Check Plugins.
     :print               Print result of ':upgrade' task instead of writing it to 'project.clj'.
  "
  [project & args]
  (let [^String t (when-let [^String t (first args)]
                    (when-not (.startsWith t ":") t))
        run-task! (get dispatch-table t)
        args (if t (rest args) args)]
    (when-not run-task!
      (main/abort (red "unknown task:") (str "'" t "'") "with parameters" (pr-str (vec (rest args))))
      (System/exit 1))
    (run-task! project args)))
