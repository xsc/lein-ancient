(ns ^{ :doc "List artifact version information." 
       :author "Yannick Scherer" }
  leiningen.ancient.tasks.get
  (:require [leiningen.ancient.projects :refer [collect-repositories]]
            [leiningen.ancient.cli :refer [parse-cli]]
            [ancient-clj.verbose :refer :all]
            [ancient-clj.core :as anc]
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
    (print (green "\"" (first v) "\""))
    (println)))

(defn- print-version-seq
  "Print version seq lines."
  [label vs]
  (when (seq vs)
    (let [c (count label)
          indent (apply str (repeat (+ 8 WIDTH) \space))
          label (if (>= c WIDTH) (str label ":") (str label ":" (apply str (repeat (- WIDTH c) \space))))]
      (print (str "  * " label " [ "))
      (let [vps (partition 5 5 nil (map first vs))]
        (doseq [v (first vps)] (print (pr-str v) ""))
        (doseq [vp (rest vps)] 
          (println)
          (print indent)
          (doseq [v vp] (print (pr-str v) ""))))
      (println "]"))))

(defn run-get-task!
  [project [_ package & args]]
  (if-not package
    (println "':get' expects an artifact to retrieve version information for.")
    (let [settings (-> (parse-cli args) (assoc :aggressive? true))
          {:keys [artifact-id group-id] :as artifact} (anc/artifact-map [package "RELEASE"])
          artifact-str (str group-id "/" artifact-id)]
      (with-settings settings
        (let [repos (collect-repositories project)]
          (println "Getting Version Information for" (yellow artifact-str) 
                   "from" (count repos) "Repositories ...")
          (let [vs (->> (anc/versions! settings repos artifact)
                     (sort #(v/version-compare (second %1) (second %2)))
                     (reverse))]
            (if-not (seq vs)
              (println "No versions found.")
              (let [releases (filter (complement (comp v/qualified? second)) vs)
                    snapshots (filter (comp v/snapshot? second) vs)
                    qualified (filter (comp  #(and (not (v/snapshot? %)) (v/qualified? %)) second) vs)]
                (println (str "  * " (count vs) " version(s) found."))
                (print-version "latest release" (first releases))
                (print-version "latest SNAPSHOT" (first snapshots))
                (print-version "latest qualified" (first qualified))
                (print-version-seq "all releases" releases)
                (print-version-seq "all SNAPSHOTs" snapshots)
                (print-version-seq "all qualified versions" qualified)))))))))
