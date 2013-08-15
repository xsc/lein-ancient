(ns ^{:doc "Check your Project for outdated Dependencies."
      :author "Yannick Scherer"}
  leiningen.ancient
  (:require [leiningen.ancient.tasks.check :refer [run-check-task!]]
            [leiningen.ancient.tasks.get :refer [run-get-task!]]
            [leiningen.ancient.tasks.upgrade :refer [run-upgrade-task! run-upgrade-global-task!]]
            [ancient-clj.verbose :refer :all]))

(def ^:private dispatch-table
  {"get"            run-get-task!
   "upgrade"        run-upgrade-task!
   "upgrade-global" run-upgrade-global-task!
   nil              run-check-task!})

(defn ^:no-project-needed ancient
  "Check your Projects for outdated Dependencies. 
  
   Usage:

     lein ancient [<options>]
     lein ancient get <package> [<options>]
     lein ancient upgrade [<options>]
     lein ancient upgrade-global [<options>]

   Tasks:

     get                  Retrieve artifact information from Maven repositories.
     upgrade              Replace artifacts in your 'project.clj' with newer versions.
     upgrade-global       Replace plugins in '~/.lein/profiles.clj' with newer versions.

   Commandline Options:
  
     :all                 Check Dependencies and Plugins.
     :dependencies        Check Dependencies. (default)
     :plugins             Check Plugins.
     :no-profiles         Do not check Dependencies/Plugins in Profiles.
     :allow-qualified     Allow '*-alpha*' versions & co. to be reported as new.
     :allow-snapshots     Allow '*-SNAPSHOT' versions to be reported as new.
     :check-clojure       Include Clojure (org.clojure/clojure) in checks.
     :aggressive          Check all available repositories (= Do not stop after first artifact match).
     :interactive         Run ':upgrade' in interactive mode, prompting whether to apply changes.
     :print               Print result of ':upgrade' task instead of writing it to 'project.clj'.
     :verbose             Produce progress indicating messages.
     :no-colors           Disable colorized output.
  "
  [project & args]
  (let [^String t (when-let [^String t (first args)]
                    (when-not (.startsWith t ":") t))
        run-task! (get dispatch-table t)]
    (when-not run-task!
      (println (red "unknown task:") (str "'" t "'") "with parameters" (pr-str (vec (rest args))))
      (System/exit 1))
    (run-task! project args)))
