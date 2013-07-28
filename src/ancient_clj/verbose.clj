(ns ^{:doc "Verbose Logic"
      :author "Yannick Scherer"}
  ancient-clj.verbose
  (:require [colorize.core :as colorize :only [yellow green red]]))

;; ## Colorize

(def ^:dynamic *colors* true)

(defmacro ^:private conditional-colors
  [& ids]
  `(do
     ~@(for [id ids]
         `(defn ~(symbol (name id))
            [& msg#]
            (let [msg# (apply str msg#)]
              (if *colors*
                (~id msg#)
                msg#))))))

(conditional-colors colorize/yellow colorize/green colorize/red)

;; ## Verbose

(def ^:dynamic *verbose* nil)

(defn verbose
  "Write Log Message."
  [& msg]
  (when *verbose*
    (binding [*out* *err*]
      (println "(verbose)" (apply str msg)))))

;; ## Settings

(defmacro with-settings
  [settings & body]
  `(let [s# ~settings]
     (binding [*verbose* (:verbose s#)
               *colors* (not (:no-colors s#))]
       ~@body)))
