(ns ^{:doc "Version Utilities for lein-ancient"
      :author "Yannick Scherer"}
  leiningen.ancient.version)

(def ^:private VERSION_REGEX 
  #"^([0-9]+)\.([0-9]+)(\.([0-9]+))?(\-(.+))?$")

(defn version-map
  "Create version map from version string. Uses everything after the first
   non-number-or-dot character as `:qualifier`, storing a vector of version numbers
   in `:version`."
  [^String version]
  (let [^String v (first (.split version "[^0-9.]" 2))
        ^String q (let [^String q (.substring version (count v))]
                    (if (.startsWith q "-")
                      (.substring q 1)
                      q))
        ^String q (when (seq q) (.toLowerCase q))]
    (-> {}
      (assoc :version-str version)
      (assoc :qualifier q)
      (assoc :version (map #(Integer/parseInt ^String %) (.split v "\\."))))))

(defn- qualifier-compare
  "Compare two qualifier strings. This tries to introduce numeric comparison
   when using qualifiers like 'alpha5' and 'alpha12'"
  [q0 q1]
  (cond (= q0 q1)          0
        (not q0)           1
        (not q1)          -1
        (= q0 "snapshot")  1
        (= q1 "snapshot") -1
        :else (or 
                (when-let [[_ qa ra] (re-find (re-matcher #"^([a-zA-Z_-]*)([0-9]+)$" q0))]
                  (when-let [[_ qb rb] (re-find (re-matcher #"^([a-zA-Z_-]*)([0-9]+)$" q1))]
                    (when (= qa qb)
                      (if (< (Integer/parseInt ra) (Integer/parseInt rb))
                        -1
                        1))))
                (.compareTo q0 q1))))

(defn- version-map-compare
  "Compare two version maps."
  [m0 m1]
  (let [v0 (:version m0)
        v1 (:version m1)
        v0 (if (< (count v0) (count v1)) (concat v0 (repeat 0)) v0)
        v1 (if (< (count v1) (count v0)) (concat v1 (repeat 0)) v1)
        version-zip (map vector v0 v1)
        version-compare (if-let [[d0 d1] (some #(when-not (= (first %) (second %)) %) version-zip)]
                          (compare d0 d1)
                          0)]
    (if (zero? version-compare)
      (qualifier-compare (:qualifier m0) (:qualifier m1))
      version-compare)))

(defn version-outdated?
  "Check if the first version is outdated."
  [v0 v1]
  (= -1 (version-map-compare v0 v1)))

(defn version-compare
  "Compare two version strings."
  [v0 v1]
  (version-map-compare (version-map v0) (version-map v1)))

(defn version-sort 
  "Sort Version Maps."
  [version-maps]
  (sort version-map-compare version-maps))

(defn snapshot?
  "Check if the given version is a SNAPSHOT."
  [v]
  (= (:qualifier v) "snapshot"))
