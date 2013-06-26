(ns ^{:doc "Version Utilities for lein-ancient"
      :author "Yannick Scherer"}
  leiningen.ancient.version)

;; ## Version Vector
;;
;; The logic given [here](http://docs.codehaus.org/display/MAVEN/Versioning) is used
;; to convert a string into a version vector that consists of:
;; 
;; - integers
;; - strings
;; - subvectors
;;
;; A "-" character creates a subvector, a "." splits elements in the current subvector.

(defn- version-split-by-change
  "Split a given string into char-only and int-only parts."
  [^String v]
  (loop [^String v v
         result []]
    (if (seq v)
      (let [[c & rst] v
            split-rx (if (Character/isDigit c) "[^0-9]" "[0-9]")
            split-result (.split v split-rx 2)
            first-part (first split-result)
            rest-part (.substring v (count first-part))]
        (recur rest-part (conj result first-part)))
      result)))

(defn- version-split-component
  "Split version component on '.' and into char-only and int-only parts."
  [^String v]
  (let [by-dot (.split v "\\.")
        by-change (mapcat version-split-by-change by-dot)]
    (map 
      (fn [^String x]
        (try
          (Integer/parseInt x)
          (catch Exception _ 
            (.toLowerCase x))))
      by-change)))

(defn- version-split
  "Split version string into version vector."
  [^String version]
  (let [by-dash (seq (.split version "-"))]
    (reduce
      (fn [r version-component]
        (concat
          (version-split-component version-component)
          (when r (vector r))))
      nil
      (reverse by-dash))))

;; ## Version Comparison
;;
;; Comparison is done using the following table:
;;
;;         |  Integer  |  String    |   List            |        Nil
;; ---------------------------------------------------------------------------
;; Integer |  compare  |    1       |         1         | compare w/ 0
;; String  |    -1     |  compare   |        -1         | compare w/ ""
;; List    |    -1     |    1       |      compare      | compare w/ [nil ...]
;; Nil     | compare 0 | compare "" | compare [nil ...] |      -
;;
;; Additionally, there is a list of qualifiers with fixed ordering.

(def ^:private QUALIFIERS
  "Order Map for well-known Qualifiers."
  { "alpha"     0 "a"         0
    "beta"      1 "b"         1
    "milestone" 2 "m"         2
    "rc"        3 "cr"        3
    "snapshot"  5
    ""          6 "final"     6 "stable"    6 })

(defn- version-component-type 
  "Get Keyword Identifying Component Type."
  [v]
  (cond (nil? v)     :nil
        (integer? v) :int
        (string? v)  :str
        :else        :list))

(defn- version-string-compare
  "Compare string components of a version vector."
  [^String s0  ^String s1]
  (let [m0 (get QUALIFIERS s0)
        m1 (get QUALIFIERS s1)
        cr (cond (and m0 m1) (compare m0 m1)
                 m0 1
                 m1 -1
                 :else (compare s0 s1))]
    (cond (neg? cr) -1
          (pos? cr)  1
          :else      0)))

(defn- version-vector-compare
  "Compare Version Vectors."
  [v0 v1]
  (let [types (map version-component-type [v0 v1])] 
    (condp = types
      [:list :list] (let [v0* (if (< (count v0) (count v1)) (concat v0 (repeat nil)) v0)
                          v1* (if (< (count v1) (count v0)) (concat v1 (repeat nil)) v1)]
                      (or
                        (some
                          (fn [[c0 c1]]
                            (let [r (version-vector-compare c0 c1)]
                              (when-not (zero? r)
                                r)))
                          (map vector v0* v1*))
                        0))
      [:int  :int]  (compare v0 v1)
      [:int  :str]  1
      [:int  :list] 1
      [:int  :nil]  (if (zero? v0) 0 1)
      [:str  :int]  -1
      [:str  :str]  (version-string-compare v0 v1)
      [:str  :list] -1
      [:str  :nil]  (version-vector-compare v0 "")
      [:list :int]  -1
      [:list :str]  1
      [:list :nil]  (version-vector-compare v0 (repeat (count v0) nil))
      [:nil :int]   (if (zero? v1) 0 -1)
      [:nil :str]   (version-vector-compare "" v1)
      [:nil :list]  (version-vector-compare (repeat (count v1) nil) v1)
      (throw (Exception. "Invalid Version Vector")))))

;; ## Version Map

(defn version-map
  "Create version map from version string. Uses everything after the first
  non-number-or-dot character as `:qualifier`, storing a vector of version numbers
  in `:version`."
  [^String version]
  (-> {}
    (assoc :version-str version)
    (assoc :version (version-split version))))

(defn- version-map-compare
  "Compare two version maps."
  [m0 m1]
  (version-vector-compare (:version m0) (:version m1)))

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
  (some
    (fn [x]
      (cond (string? x) (= x "snapshot")
            (integer? x) nil
            :else (snapshot? {:version x})))
    (:version v)))
