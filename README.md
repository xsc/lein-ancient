# ancient-clj

__ancient-clj__ is a library for accessing versioning metadata in Maven repositories.
It is the base for the Leiningen plugin [lein-ancient](https://github.com/xsc/lein-ancient).

[![Build Status](https://travis-ci.org/xsc/ancient-clj.svg?branch=ys%2Frefactor)](https://travis-ci.org/xsc/ancient-clj)
[![endorse](https://api.coderwall.com/xsc/endorsecount.png)](https://coderwall.com/xsc)

Version comparison is done using [version-clj](https://github.com/xsc/version-clj).

## Usage

__Leiningen ([via Clojars](https://clojars.org/ancient-clj))__

[![Clojars Project](http://clojars.org/ancient-clj/latest-version.svg)](http://clojars.org/ancient-clj)

__REPL__

```clojure
(require '[ancient-clj.core :as ancient])
```

### Repositories

The base of all ancient-clj operations is a map associating an ID with a repository specification, given
as one of the following:

- a URI string (`http://...`, `https://...`, `file://...`, `s3p://...`),
- a map of `:uri` and repository-specific settings (`:username`, `:passphrase`, ...),
- a two-parameter function returning a seq of version strings based on artifact group and ID.

(`:uri` and `:url`, as well as `:passphrase` and `:password` are interchangeable.)

__Example:__

```clojure
{"central"   "http://repo1.maven.org/maven2"
 "clojars"   "https://clojars.org/repo"
 "http-auth" {:uri "https://my.repo.server/releases"
              :username "HTTP_BASIC_AUTH_USER"
              :passphrase "HTTP_BASIC_AUTH_PASSWORD"}
 "s3"        {:uri "s3p://maven/bucket"
              :username "AWS_ACCESS_KEY"
              :passphrase "AWS_SECRET_KEY"}}
```

The default repositories are stored in `ancient-clj.core/default-repositories`.

### Artifacts + Options

Artifacts can be given as everything implementing `ancient-clj.artifact/Artifact`:

- a vector (`[ancient-clj "0.2.0"]`, `[ancient-clj]`, ...)
- a symbol/keyword/string (`ancient-clj`, `:ancient-clj/ancient-clj`, `"ancient-clj"`)
- a map of `:group`, `:id` and `:version-string`.

All ancient-clj functions take one of those as first parameter, as well as an optional map of
options as second one:

- `:snapshots?`: whether or not to consider SNAPSHOT versions in the results (default: true),
- `:qualified?`: whether or not to consider alpha/beta/RC/... versions in the results
  (default: true),
- `:sort`: how to sort the results (`:desc` (default), `:asc`, `:none`),
- `:repositories`: see above (default: Maven Central + Clojars).

To analyze an artifact use `ancient-clj.core/read-artifact`:

```clojure
(ancient/read-artifact '[com.taoensso/timbre "3.3.1"])
;; => {:form [com.taoensso/timbre "3.3.1"],
;;     :symbol com.taoensso/timbre,
;;     :version-string "3.3.1",
;;     :version [(3 3 1)],
;;     :id "timbre",
;;     :group "com.taoensso"}
```

### Operations

The base of all operations is `versions-per-repository!` which produces a map of either
a seq of version maps or a Throwable associated with each repository ID:

```clojure
(ancient/versions-per-repository! 'ancient-clj)
;; => {"clojars" ({:version [(0 1 10)], :version-string "0.1.10"} ...)
;;     "central" (...)}

(ancient/versions-per-repository!
  'ancient-clj
  {:repositories {"invalid" "http://nosuchpage.maven.org"}})
;; => {"invalid" #<UnknownHostException java.net.UnknownHostException: ...>}
```

As you can see, versions are given as a map of `:version-string` and `:version` (a version-clj
version value).

Flat seqs of version maps/strings can be obtained using `versions!` and `version-strings!`, the
latest ones are returned by `latest-version!` and `latest-version-string!`.

`artifact-outdated?` and `artifact-outdated-string?` only return a version value if it is
of a more recent version than the input artifact.

## Supported Repository Types

- HTTP/HTTPS Repositories
- Local Repositories
- [Amazon S3 Repositories](https://github.com/technomancy/s3-wagon-private) (private)

## License

```
Copyright (c) 2015 Yannick Scherer

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
