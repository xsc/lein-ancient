(ns ^{ :doc "ancient-clj API"
       :author "Yannick Scherer" }
  ancient-clj.core
  (:require [ancient-clj.repository :as r]
            [version-clj.core :as v]))

;; ## Data Structures

(defn artifact-map
  "Create artifact map (:group-id, :artifact-id, :version)."
  [[dep version & _]]
  (let [dep (str dep)
        [g a] (if (.contains dep "/")
                (.split dep "/" 2)
                [dep dep])] 
    (-> {}
      (assoc :group-id g)
      (assoc :artifact-id a)
      (assoc :version [version (v/version->seq version)]))))

;; ## Core Functionality
;;
;; This operates on artifact vectors (`[artifact version ...]`) rather than separate
;; group and artifact IDs.

(defmacro ^:private defancient
  "Create function that takes an optional settings map and/or repository seq as first/second
   parameters."
  [id docstring f]
  (let [[s r a] ['settings 'repos 'artifact-vector]]
    `(defn ~id ~docstring
       ([~a] (~id nil r/*repositories* ~a))
       ([~r ~a] (~id (when (map? ~r) ~r) (if (map? ~r) r/*repositories* ~r) ~a))
       ([~s ~r ~a] (let [m# (artifact-map ~a)]
                     (~f ~s ~r (:group-id m#) (:artifact-id m#)))))))

(defancient versions!
  "Retrieve seq of version pairs for the given artifact vector."
  r/retrieve-versions!)

(defancient version-strings!
  "Retrieve seq of version strings for the given artifact vector."
  r/retrieve-version-strings!)

(defancient latest-version!
  "Retrieve latest version for the given artifact (`[artifact version ...]`). `settings` and
   `repos` have the same semantics as in `ancient-clj.repository/retrieve-versions!`."
  r/retrieve-latest-version!)

(defancient latest-version-string!
  "Retrieve latest version string for the given artifact (`[artifact version ...]`). `settings` and
   `repos` have the same semantics as in `ancient-clj.repository/retrieve-versions!`."
  r/retrieve-latest-version-string!)

(defn artifact-outdated?
  "Check if the given artifact (`[artifact version ...]`) is outdated. Return the latest version pair
   if the given version is lower than the retrieved one; `nil` otherwise."
  ([artifact-vector] (artifact-outdated? nil r/*repositories* artifact-vector))
  ([repos artifact-vector]
   (artifact-outdated?
     (when (map? repos) repos)
     (if (map? repos) r/*repositories* repos)
     artifact-vector))
  ([settings repos artifact-vector]
   (let [{:keys [group-id artifact-id version]} (artifact-map artifact-vector)
         [_ v0] version]
     (when-let [[_ v1 :as latest] (r/retrieve-latest-version! settings repos group-id artifact-id)]
       (when (= -1 (v/version-seq-compare v0 v1))
         latest)))))
