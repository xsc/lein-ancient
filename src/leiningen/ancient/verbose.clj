(ns leiningen.ancient.verbose
  (:require [leiningen.core.main :as main]
            [jansi-clj.core :as color]))

(def lock
  (Object.))

(defmacro verbosef
  [& fmt]
  `(locking lock
     (main/info (format ~@fmt))))

(defmacro debugf
  [& fmt]
  `(locking lock
     (main/debug "(debug)" (format ~@fmt))))

(defmacro infof
  [& fmt]
  `(locking lock
     (main/info "(info) " (format ~@fmt))))

(defmacro warnf
  [& fmt]
  `(locking lock
     (main/warn (color/yellow "(warn) ") (format ~@fmt))))

(defmacro errorf
  [& fmt]
  `(locking lock
     (main/warn (color/red "(error)") (format ~@fmt))))
