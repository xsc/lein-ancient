(ns ^{:doc "Check your Project for outdated Dependencies."
      :author "Yannick Scherer"}
  leiningen.ancient
  (:require [leiningen.ancient.tasks.check :refer [run-check-task!]]
            [leiningen.ancient.tasks.get :refer [run-get-task!]]
            [leiningen.ancient.tasks.update-project :refer []]))

(defn ^:no-project-needed ancient
  "Check your Projects for outdated Dependencies. 
  
   Usage:

     lein ancient :get <package> [<options>]
     lein ancient [<options>]

   Commandline Options:
  
     :all                 Check Dependencies and Plugins.
     :dependencies        Check Dependencies. (default)
     :plugins             Check Plugins.
     :no-profiles         Do not check Dependencies/Plugins in Profiles.
     :allow-qualified     Allow '*-alpha*' versions & co. to be reported as new.
     :allow-snapshots     Allow '*-SNAPSHOT' versions to be reported as new.
     :check-clojure       Include Clojure (org.clojure/clojure) in checks.
     :aggressive          Check all available repositories (= Do not stop after first artifact match).
     :verbose             Produce progress indicating messages.
     :no-colors           Disable colorized output.
  "
  [project & args]
  (condp = (first args)
    ":get" (run-get-task! project args)
    (run-check-task! project args)))
