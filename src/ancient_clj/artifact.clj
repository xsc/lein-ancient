(ns ancient-clj.artifact
  (:require [potemkin :refer [defprotocol+]]
            [version-clj.core :as v]))

;; ## Protocol

(defprotocol+ Artifact
  "Protocol for artifact representations."
  (read-artifact [this]
    "Create a map of ':group', ':id', ':version' and ':version-string',
     as well as `:symbol` and `:form`."))

;; ## Helpers

(defn- parse-id
  [s]
  (let [[g a] (if (string? s)
                (if (.contains s "/")
                  (.split s "/" 2)
                  [s s])
                (let [id (name s)]
                  (if-let [n (namespace s)]
                    [n id]
                    [id id])))]
    {:id     a
     :group  g}))

(defn- parse-version
  [version]
  (let [v (str version)]
    {:version        (v/version->seq v)
     :version-string v}))

(defn- ->artifact
  [{:keys [group id] :as id-map}
   {:keys [version-string] :as v-map}]
  {:pre [(string? group)
         (string? id)]}
  (let [sym (if (= group id)
              (symbol id)
              (symbol group id))]
    (-> (merge id-map v-map)
        (assoc :symbol sym)
        (assoc :form [sym version-string]))))

;; ## Implementations

(extend-protocol Artifact
  clojure.lang.IPersistentVector
  (read-artifact [v]
    {:pre [(seq v)]}
    (let [[id version] v]
      (->artifact
        (parse-id id)
        (parse-version version))))

  clojure.lang.IPersistentMap
  (read-artifact [{:keys [version-string] :as v}]
    (->artifact
      v
      (parse-version version-string)))

  clojure.lang.Named
  (read-artifact [n]
    (read-artifact [n]))

  String
  (read-artifact [n]
    (read-artifact [n])))
