(ns ^{ :doc "Repository Access for ancient-clj."
       :author "Yannick Scherer" }
  ancient-clj.repository
  (:require [ancient-clj.repository.core :as r]
            [ancient-clj.repository.http]
            [ancient-clj.repository.local]
            [ancient-clj.repository.s3p]
            [ancient-clj.verbose :refer [verbose warn]]
            [clojure.data.xml :as xml :only [parse-str]]
            [version-clj.core :as v]))

;; ## Wrapper

(def repository
  "Create entity implementing the `Repository` protocol for either a
   repository URL or a repository map."
  (memoize
    (fn [r]
      (if (string? r)
        (r/create-repository {:url r})
        (r/create-repository r)))))

(extend-protocol r/Repository
  java.lang.String
  (retrieve-metadata-xml! [this group-id artifact-id]
    (when-let [repo (repository {:url this})]
      (r/retrieve-metadata-xml! repo group-id artifact-id)))
  clojure.lang.IPersistentMap
  (retrieve-metadata-xml! [this group-id artifact-id]
    (when-let [repo (repository this)]
      (r/retrieve-metadata-xml! repo group-id artifact-id))))

;; ## Standard Repositories

(def ^:dynamic *repositories*
  "Repositories to use for metadata retrieval if none are given."
  (vector
    (repository "http://repo1.maven.org/maven2")
    (repository "https://clojars.org/repo")))

;; ## Access Functions

(defn- retrieve-single-metadata-xml!
  [repo group-id artifact-id]
  (try
    (r/retrieve-metadata-xml! repo group-id artifact-id)
    (catch java.io.FileNotFoundException _ nil)
    (catch javax.net.ssl.SSLException ex
      (warn "An SSL error occured when retrieving '" group-id "/" artifact-id "':\n"
            (.getMessage ex)))
    (catch Exception ex
      (warn "An unexpected error occured when retrieving '" group-id "/" artifact-id "':\n"
            (.getMessage ex)))))

(defn retrieve-metadata-xml!
  "Retrieve the version metadata XML file as a String searching the given
   repositories. The first found result will be returned."
  ([group-id artifact-id]
   (retrieve-metadata-xml! *repositories* group-id artifact-id))
  ([repos group-id artifact-id]
   (loop [repos repos]
     (when (seq repos)
       (or
         (retrieve-single-metadata-xml! (first repos) group-id artifact-id)
         (recur (rest repos)))))))

(defn retrieve-versions!
  "Retrieve a seq of version pairs (`[version-string version-seq]`) for the given artifact
   from the given repositories. The following calls are possible (using an optional
   settings map):

     (retrieve-versions! [r1 r2] group artifact)
     (retrieve-versions! {:aggressive? true} [r1 r2] group artifact)

   The second call will not stop after the first metadata match. By default, the keys
   `:snapshots?` and `:qualified` of the settings map are set to `true`."
  ([artifact-id] (retrieve-versions! nil *repositories* artifact-id artifact-id))
  ([group-id artifact-id]
   (retrieve-versions! nil *repositories* group-id artifact-id))
  ([repos group-id artifact-id]
   (retrieve-versions!
     (if (map? repos) repos nil)
     (if (map? repos) *repositories* repos)
     group-id artifact-id))
  ([{:keys [aggressive? snapshots? qualified?] :as settings} repos group-id artifact-id]
   (let [aggressive? (:aggressive? settings true)
         snapshots? (:snapshots? settings true)
         qualified? (:qualified? settings true)]
     (->>
       (loop [repos repos
              versions nil]
         (if-not (seq repos)
           versions
           (let [[repo & rst] repos]
             (if-let [repo-versions (when-let [mta (retrieve-single-metadata-xml! repo group-id artifact-id)]
                                      (when (string? mta)
                                        (for [t (try
                                                  (xml-seq (xml/parse-str mta))
                                                  (catch Exception e
                                                    (verbose "Could not read XML: " (.getMessage e))))
                                              :when (= (:tag t) :version)]
                                          (first (:content t)))))]
               (if-not aggressive?
                 repo-versions
                 (recur rst (concat versions repo-versions)))
               (recur rst versions)))))
       (map (juxt identity v/version->seq))
       (distinct)
       (filter
         (fn [[v vs]]
           (and (or snapshots? (not (v/snapshot? vs)))
                (or qualified? (v/snapshot? vs) (not (v/qualified? vs))))))))))

(defn retrieve-version-strings!
  "Retrieve version strings. Arguments are the same as for `retrieve-versions!`."
  [& args]
  (let [versions (apply retrieve-versions! args)]
    (map first versions)))

(defn retrieve-latest-version!
  "Retrieve the latest version. Arguments are the same as for `retrieve-versions!`."
  [& args]
  (let [versions (apply retrieve-versions! args)]
    (->> versions
      (sort #(v/version-seq-compare (second %1) (second %2)))
      (last))))

(def retrieve-latest-version-string!
  "Retrieve the latest version's version string. Arguments are the same as for
   `retrieve-latest-version!`."
  (comp first retrieve-latest-version!))
