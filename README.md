# lein-ancient

A [Leiningen](https://github.com/technomancy/leiningen) plugin to check your project for outdated
dependencies. Similar to [lein-outdated](https://github.com/ato/lein-outdated) but uses metadata
XML files in the different Maven repositories instead of a [Lucene](http://lucene.apache.org/core/)-based
search index.

[![Build Status](https://travis-ci.org/xsc/lein-ancient.png)](https://travis-ci.org/xsc/lein-ancient)

There is an [open pull request](https://github.com/ato/lein-outdated/pull/16) of mine at lein-outdated that 
resembles lein-ancient quite a bit. If it is accepted, any further additions to lein-ancient will end up
there and development on this project will be discontinued.

## Usage

__Leiningen__ ([via Clojars](https://clojars.org/lein-ancient))

Put the following into the `:plugins` vector of `~/.lein/profiles.clj`:

```clojure
[lein-ancient "0.1.1"]
```

__Command Line__

You can use `lein-ancient` to check the dependencies/plugins of a project and those specified
in `~/.lein/profiles.clj`.

```bash
$ lein ancient
[com.taoensso/timbre "2.1.2"] is available but we use "2.0.1"

$ lein ancient :plugins
[lein-tarsier/lein-tarsier "0.10.0"] is available but we use "0.9.4"

$ lein ancient :all
[com.taoensso/timbre "2.1.2"] is available but we use "2.0.1"
[lein-tarsier/lein-tarsier "0.10.0"] is available but we use "0.9.4"
```

## License

Copyright &copy; 2013 Yannick Scherer

Distributed under the Eclipse Public License, the same as Clojure.
