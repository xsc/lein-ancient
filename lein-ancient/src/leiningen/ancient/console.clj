(ns leiningen.ancient.console
  (:require [leiningen.ancient.verbose :refer :all]
            [jansi-clj.core :as color]))

(defn prompt
  "Create a yes/no prompt using the given message."
  [& msg]
  (let [msg (str (apply str msg) " [yes/no] ")]
    (locking *out*
      (loop [i 0]
        (when (= (mod i 4) 2)
          (println "*** please type in one of 'yes'/'y' or 'no'/'n' ***"))
        (print msg)
        (flush)
        (let [r (or (read-line) "")
              r (.toLowerCase ^String r)]
          (case r
            ("yes" "y") true
            ("no" "n")  false
            (recur (inc i))))))))

(defn print-outdated-message
  "Print a message indicating that a given artifact is outdated."
  [{:keys [latest artifact]}]
  {:pre [(map? latest) (map? artifact)]}
  (let [{:keys [symbol version-string]} artifact]
    (verbosef
      "[%s %s] is available but we use %s"
      symbol
      (color/green (pr-str (:version-string latest)))
      (color/yellow (pr-str version-string)))))

(defn print-ignored-message
  [{:keys [latest artifact keys]}]
  (let [{:keys [symbol version-string]} artifact
        clojure? (contains? (set keys) :clojure)]
    (verbosef
      "[%s %s] is available but we use %s %s"
      symbol
      (color/green (pr-str (:version-string latest)))
      (color/yellow (pr-str version-string))
      (color/blue
        (if clojure?
          "(use :check-clojure to upgrade)"
          "(ignored)")))))
