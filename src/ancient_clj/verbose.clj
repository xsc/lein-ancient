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

;; ## String Creation

(defn version-string
  [version]
  (str "\"" (first version) "\""))

(defn artifact-string
  [group-id artifact-id version]
  (let  [f  (if  (= group-id artifact-id)
              artifact-id
              (str group-id "/" artifact-id))]
    (str "[" f " "  (green  (version-string version)) "]")))

;; ## Settings

(defmacro with-settings
  [settings & body]
  `(let [s# ~settings]
     (binding [*verbose* (:verbose s#)
               *colors* (not (:no-colors s#))]
       ~@body)))
