# ancient-clj

__ancient-clj__ is a library for accessing versioning metadata in Maven repositories. 
It is the base for the Leiningen plugin [lein-ancient](https://github.com/xsc/lein-ancient).

[![Build Status](https://travis-ci.org/xsc/ancient-clj.png)](https://travis-ci.org/xsc/ancient-clj)
[![endorse](https://api.coderwall.com/xsc/endorsecount.png)](https://coderwall.com/xsc)

Version comparison is done using [version-clj](https://github.com/xsc/version-clj).

## Usage

__Leiningen ([via Clojars](https://clojars.org/ancient-clj))__

```clojure
[ancient-clj "0.1.0"]
```

__REPL__

```clojure
(require '[ancient-clj.core :as anc])

;; an artifact can be split into its parts (:group-id, :artifact-id, :version)
(anc/artifact-map '[ancient-clj "0.1.0"]) ;; => { :group-id "ancient-clj" ... }

;; artifact metadata can either be retrieved using the artifact ID ...
(anc/versions! 'ancient-clj)              
;;   => (["0.1.0-SNAPSHOT" [(0 1 0) ("snapshot")]])
(anc/version-strings! 'ancient-clj)       
;;   => ("0.1.0-SNAPSHOT")
(anc/latest-version! 'ancient-clj)        
;;   => ["0.1.0-SNAPSHOT" [(0 1 0) ("snapshot")]]
(anc/latest-version-string! 'ancient-clj) 
;;   => "0.1.0-SNAPSHOT"

;; ... or the artifact vector.
(anc/versions! '[ancient-clj "0.1.0"])    
;;   => (["0.1.0-SNAPSHOT" [(0 1 0) ("snapshot")]])

;; You can use an optional settings map with all the above functions, ...
(anc/latest-version-string! 'lein-ancient)                     ;; => "0.4.3-SNAPSHOT"
(anc/latest-version-string! {:snapshots? false} 'lein-ancient) ;; => "0.4.2"
(anc/latest-version-string! {:qualified? false} 'lein-ancient) ;; => "0.4.3-SNAPSHOT"

;; ... a list of repositories to check ...
(def repos ["https://clojars.org/repo"
            "http://repo1.maven.org/maven2"
            "https://oss.sonatype.org/content/groups/public/"])

(anc/latest-version-string! 'org.clojure/clojure)       ;; => "1.5.1"
(anc/latest-version-string! repos 'org.clojure/clojure) ;; => "1.6.0-master-SNAPSHOT"

;; ... or both.
(anc/latest-version-string! {:snapshots? false} repos 'org.clojure/clojure)  
;;   => "1.5.1"

;; By default, all operations are "aggressive", i.e. they check all given repositories;
;; you can make metadata retrieval stop after the first repository that returns a valid
;; result (in our case "http://clojars.org/repo" with a rather old Clojure version):
(anc/latest-version-string! {:aggressive? false} repos 'org.clojure/clojure) 
;;   => "1.5.0-alpha3"

;; The function 'artifact-outdated?' can check whether a given artifact has newer 
;; versions available. It has to be called with an artifact vector and takes a settings 
;; map or repository seq as well, returning either `[version-string version-seq]` 
;; (see version-clj) or `nil`:
(anc/artifact-outdated? '[lein-ancient "0.4.2"])                     
;;   => ["0.4.3-SNAPSHOT" ...]
(anc/artifact-outdated? {:snapshots? false} '[lein-ancient "0.4.2"]) 
;;   => nil
(anc/artifact-outdated-string? repos '[org.clojure/clojure "1.5.1"]) 
;;   => "1.6.0-master-SNAPSHOT"
```

## Supported Repository Types

- HTTP/HTTPS Repositories
- Local Repositories
- [Amazon S3 Repositories](https://github.com/technomancy/s3-wagon-private) (private)

## License

Copyright &copy; Yannick Scherer

Distributed under the Eclipse Public License, the same as Clojure.
