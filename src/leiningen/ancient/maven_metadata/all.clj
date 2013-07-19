(ns ^{ :doc "Load all metadata retriever generators."
       :author "Yannick Scherer" }
  leiningen.ancient.maven-metadata.all)

(require '[leiningen.ancient.maven-metadata http local s3p])
