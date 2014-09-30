(ns ancient-clj.io.xml
  (:require [clojure.data.xml :as xml]
            [version-clj.core :as v]))

(defn metadata-xml->versions
  "Parse an XML string and read all version strings contained within. Returns
   a seq of version strings."
  [^String xml-string]
  {:post [(every? string? %)]}
  (try
    (let [elements (->> (xml/parse-str xml-string)
                        (xml-seq)
                        (vec))]
      (->> (for [{:keys [tag content]} elements
                 :when (= tag :version)]
             (first content))
           (distinct)))
    (catch Throwable ex
      (throw
        (Exception.
          (format
            "Could not parse metadata XML. %s"
            (.getMessage ex))
          ex)))))

(defn metadata-path
  "Create path to XML file based on group and ID, as well as an optional
   XML filename."
  [group id & [filename]]
  (let [group-path (.replaceAll ^String group "\\." "/")]
    (format "%s/%s/%s"
            group-path
            id
            (or filename "maven-metadata.xml"))))

(defn metadata-uri
  "Create URI to XML file based on base URL, group and ID, as well as an
   optional XML filename."
  [uri group id & [filename]]
  (str uri
       (if-not (.endsWith ^String uri "/")
         "/")
       (metadata-path group id filename)))
