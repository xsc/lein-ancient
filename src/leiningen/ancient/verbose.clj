(ns ^{:doc "Verbose Logic for lein-ancient" 
      :author "Yannick Scherer"}
  leiningen.ancient.verbose
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
