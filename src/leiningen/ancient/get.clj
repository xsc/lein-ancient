(ns leiningen.ancient.get
  (:refer-clojure :exclude [get])
  (:require [leiningen.ancient
             [utils :as utils]
             [verbose :refer :all]]
            [jansi-clj.core :as color]
            [ancient-clj.core :as ancient]
            [version-clj.core :as v]))

(def ^:private ^:const WIDTH
  "Maximum width of labels for `:get`."
  23)

(defn- print-version
  "Print version number line."
  [label v]
  (when v
    (print (str "  * " label ": "))
    (print (apply str (repeat (- WIDTH (count label)) \space)))
    (print (color/green "\"" (:version-string v) "\""))
    (println)))

(defn- print-version-seq
  "Print version seq lines."
  [label vs]
  (when (seq vs)
    (let [c (count label)
          indent (apply str (repeat (+ 8 WIDTH) \space))
          label (if (>= c WIDTH) (str label ":") (str label ":" (apply str (repeat (- WIDTH c) \space))))]
      (print (str "  * " label " [ "))
      (let [vps (partition 5 5 nil (map :version-string vs))]
        (doseq [v (first vps)] (print (pr-str v) ""))
        (doseq [vp (rest vps)]
          (println)
          (print indent)
          (doseq [v vp] (print (pr-str v) ""))))
      (println "]"))))

(defn- print-all-versions
  [vs]
  (let [releases (filter (complement (comp v/qualified? :version)) vs)
        snapshots (filter (comp v/snapshot? :version) vs)
        qualified (filter (comp  #(and (not (v/snapshot? %)) (v/qualified? %)) :version) vs)]
    (println)
    (println (str "  * " (count vs) " version(s) found."))
    (print-version "latest release" (first releases))
    (print-version "latest SNAPSHOT" (first snapshots))
    (print-version "latest qualified" (first qualified))
    (print-version-seq "all releases" releases)
    (print-version-seq "all SNAPSHOTs" snapshots)
    (print-version-seq "all qualified versions" qualified)
    (println)))

(utils/deftask show-versions
  {:docstring "List all versions for a given artifact."
   :exclude [:all :allow-all :allow-snapshots :allow-qualified
             :check-clojure :interactive :plugins :no-profiles
             :no-tests :tests :print :recursive]
   :fixed {:snapshots? true
           :qualified? true
           :dependencies? true
           :plugins? true
           :java-agents? true}}
  [{:keys [artifact-opts args]}]
  (if-let [artifact' (first args)]
    (let [{:keys [symbol] :as artifact} (ancient/read-artifact artifact')]
      (verbosef "retrieving versions for %s from %d repositories ..."
                (color/yellow symbol)
                (count (:repositories artifact-opts)))
      (let [vs (ancient/versions! artifact artifact-opts)]
        (if (seq vs)
          (print-all-versions vs)
          (verbosef "no versions found."))))
    (errorf "no artifact coordinates given.")))

(utils/deftask show-latest
  {:docstring "Print a version vector for the given artifact's latest release."
   :exclude [:all :check-clojure :interactive :plugins
             :no-profiles :no-tests :tests :print :recursive]
   :fixed {:dependencies? true
           :plugins? true
           :java-agents? true}}
  [{:keys [artifact-opts args]}]
  (if-let [artifact' (first args)]
    (let [{:keys [symbol] :as artifact} (ancient/read-artifact artifact')
          vs (ancient/latest-version-string! artifact artifact-opts)]
      (if vs
         (println (format "[%s %s]" symbol (pr-str vs)))
         (println "no versions found.")))
    (errorf "no artifact coordinates given.")))
