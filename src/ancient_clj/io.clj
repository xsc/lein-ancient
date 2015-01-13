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

(def ^:private get-scheme
  (let [m {:https :http
           :s3p   :s3}]
    (fn [^String uri]
      (let [scheme (some-> (string/split uri #":/" 2)
                           (first)
                           (keyword))]
        (m scheme scheme)))))

(defmulti ^:private loader-for*
  (fn [{:keys [uri]}]
    (when (string? uri)
      (get-scheme uri)))
  :default nil)

(defn- wrapped-loader-for
  [{:keys [uri f wrap]
    :or {wrap identity}
    :as opts}]
  (let [loader (cond uri (loader-for* opts)
                     f   f
                     :else (throw
                             (Exception.
                               (format "cannot create loader from: %s"
                                       (pr-str opts)))))]
    (wrap loader)))

(defn loader-for
  [v]
  (cond (fn? v) v
        (string? v) (wrapped-loader-for {:uri v})
        (map? v)  (-> v
                      (rename-keys
                        {:url :uri
                         :password :passphrase})
                      (wrapped-loader-for))
        :else (throw
                (Exception.
                  (format "cannot create loader from: %s" (pr-str v))))))

;; ## Loaders

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
