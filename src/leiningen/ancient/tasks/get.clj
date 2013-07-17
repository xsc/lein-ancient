(ns ^{ :doc "List artifact version information." 
       :author "Yannick Scherer" }
  leiningen.ancient.tasks.get
  (:require [leiningen.ancient.verbose :refer :all]
            [leiningen.ancient.maven-metadata :refer :all]
            [leiningen.ancient.maven-metadata http local s3p]  
            [leiningen.ancient.version :refer [version-sort version-map snapshot? qualified?]]
            [leiningen.ancient.projects :refer [dependency-map]]
            [leiningen.ancient.cli :refer [parse-cli]]))

(def ^:private ^:const WIDTH
  "Maximum width of labels for `:get`."
  23)

(defn- print-version
  "Print version number line."
  [label v]
  (when v
    (print (str "  * " label ": "))
    (print (apply str (repeat (- WIDTH (count label)) \space)))
    (print (green "\"" (:version-str v) "\""))
    (println)))

(defn- print-version-seq
  "Print version seq lines."
  [label vs]
  (when (seq vs)
    (let [c (count label)
          indent (apply str (repeat (+ 8 WIDTH) \space))
          label (if (>= c WIDTH) (str label ":") (str label ":" (apply str (repeat (- WIDTH c) \space))))]
      (print (str "  * " label " [ "))
      (let [vps (partition 5 5 nil (map :version-str vs))]
        (doseq [v (first vps)] (print (pr-str v) ""))
        (doseq [vp (rest vps)] 
          (println)
          (print indent)
          (doseq [v vp] (print (pr-str v) ""))))
      (println "]"))))

(defn run-get-task!
  [project [_ package & args]]
  (if-not package
    (println "':get' expects a package to retrieve version information for.")
    (let [settings (-> (parse-cli args)
                     (assoc :aggressive true))
          {:keys [artifact-id group-id]} (dependency-map [package ""])
          artifact-str (str group-id "/" artifact-id)]
      (binding [*verbose* (:verbose settings)
                *colors* (not (:no-colors settings))]
        (let [retrievers (collect-metadata-retrievers project)]
          (println "Getting Version Information for" (yellow artifact-str) 
                   "from" (count retrievers) "Repositories ...")
          (let [vs (->> (retrieve-metadata! retrievers settings group-id artifact-id)
                     (mapcat version-seq)
                     (distinct)
                     (map version-map)
                     (version-sort)
                     (reverse))]
            (if-not (seq vs)
              (println "No versions found.")
              (let [releases (filter (complement qualified?) vs)
                    snapshots (filter snapshot? vs)
                    qualified (filter #(and (not (snapshot? %)) (qualified? %)) vs)]
                (println (str "  * " (count vs) " version(s) found."))
                (print-version "latest release" (first releases))
                (print-version "latest SNAPSHOT" (first snapshots))
                (print-version "latest qualified" (first qualified))
                (print-version-seq "all releases" releases)
                (print-version-seq "all SNAPSHOTs" snapshots)
                (print-version-seq "all qualified versions" qualified)))))))))
