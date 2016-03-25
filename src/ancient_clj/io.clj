(ns ancient-clj.io
  (:require [ancient-clj.io
             [http :refer [http-loader]]
             [local :refer [local-loader]]
             [s3 :refer [s3-loader]]]
            [clojure.set :refer [rename-keys]]
            [clojure.string :as string]
            [clojure.java.io :as io])
  (:import [java.net URL URISyntaxException]))

;; ## Loader Creation

(defmulti ^:private loader-for*
  ::scheme
  :default nil)

;; ## Helper

(defn- throw-loader-exception!
  [opts]
  (throw
    (IllegalArgumentException.
      (format "cannot create loader from: %s" (pr-str opts)))))

(def ^:private get-scheme
  (let [m {:https :http
           :s3p   :s3}]
    (fn [^String uri]
      (let [scheme (some-> (string/split uri #":/?" 2)
                           (first)
                           (keyword))]
        (m scheme scheme)))))

(defn- assoc-scheme
  [{:keys [uri] :as opts}]
  (if (string? uri)
    (assoc opts ::scheme (get-scheme uri))
    opts))

(defn- prepare-loader-options
  [opts]
  (some-> (cond (fn? opts)     {:f opts}
                (string? opts) {:uri opts}
                (map? opts)    (rename-keys
                                 opts
                                 {:url :uri
                                  :password :passphrase})
                :else nil)
          (assoc-scheme)))

(defn- wrap-loader
  [{:keys [wrap] :or {wrap identity}} loader]
  (wrap loader))

(defn- create-loader
  [{:keys [uri f] :as opts}]
  (if uri
    (loader-for* opts)
    f))

(defn maybe-loader-for
  [opts]
  (some->> (prepare-loader-options opts)
           (create-loader)
           (wrap-loader opts)))

(defn loader-for
  [opts]
  (or (maybe-loader-for opts)
      (throw-loader-exception! opts)))

;; ## Loaders

(defmethod loader-for* nil
  [_]
  nil)

(defmethod loader-for* :http
  [{:keys [uri] :as opts}]
  (http-loader uri opts))

(defmethod loader-for* :file
  [{:keys [uri] :as opts}]
  (let [url (URL. uri)]
    (-> (try
          (io/file (.toURI url))
          (catch URISyntaxException _
            (io/file (.getPath url))))
        (local-loader opts))))

(defmethod loader-for* :s3
  [{:keys [^String uri] :as opts}]
  {:pre [(re-matches #"s3p?://.*" uri)]}
  (let [[bucket path] (some-> (re-find #"s3p?://(.*)" uri)
                              ^String (second)
                              (.split "/" 2))]
    (assert (not (empty? bucket)))
    (assert (not (empty? path)))
    (s3-loader bucket (assoc opts :path path))))
