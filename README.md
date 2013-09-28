# lein-ancient

A [Leiningen](https://github.com/technomancy/leiningen) plugin to check your project for outdated
dependencies and plugins. 

[![Build Status](https://travis-ci.org/xsc/lein-ancient.png)](https://travis-ci.org/xsc/lein-ancient)
[![endorse](https://api.coderwall.com/xsc/endorsecount.png)](https://coderwall.com/xsc)

This plugin supersedes [lein-outdated](https://github.com/ato/lein-outdated) and uses metadata
XML files in the different Maven repositories instead of a [Lucene](http://lucene.apache.org/core/)-based
search index. Version comparison is done using [version-clj](https://github.com/xsc/version-clj).

## Usage

__Leiningen__ ([via Clojars](https://clojars.org/lein-ancient))

Put one of the following into the `:plugins` vector of the `:user` profile in your `~/.lein/profiles.clj`:

```clojure
[lein-ancient "0.4.4"]          ;; stable
[lein-ancient "0.5.0-alpha3"]   ;; unstable
```

This plugin is destined for Leiningen >= 2.0.0.

__Command Line__

_Note:_ The following paragraphs use the tasks/command line options for `lein-ancient` >= 0.5.0-alpha1. To
see the ones applying to your version, call `lein help ancient`.

You can use `lein-ancient` to check the dependencies/plugins of a project and those specified
in `~/.lein/profiles.clj`.

```bash
$ lein ancient
[com.taoensso/timbre "2.4.1"] is available but we use "2.1.2"
[potemkin "0.3.1"] is available but we use "0.3.0"
[pandect "0.3.0"] is available but we use "0.2.3"

$ lein ancient :allow-qualified
[com.taoensso/timbre "2.4.1"] is available but we use "2.1.2"
[potemkin "0.3.1"] is available but we use "0.3.0"
[pandect "0.3.0"] is available but we use "0.2.3"
[midje "1.6-alpha3"] is available but we use "1.5.1"

$ lein ancient :allow-snapshots
[com.taoensso/timbre "2.4.1"] is available but we use "2.1.2"
[potemkin "0.3.2-SNAPSHOT"] is available but we use "0.3.0"
[pandect "0.3.0"] is available but we use "0.2.3"
[midje "1.6-SNAPSHOT"] is available but we use "1.5.1"

$ lein ancient :plugins
[lein-tarsier/lein-tarsier "0.10.0"] is available but we use "0.9.4"

$ lein ancient :all
[com.taoensso/timbre "2.4.1"] is available but we use "2.1.2"
[potemkin "0.3.1"] is available but we use "0.3.0"
[pandect "0.3.0"] is available but we use "0.2.3"
[lein-tarsier/lein-tarsier "0.10.0"] is available but we use "0.9.4"
```

You can automatically and iteractively upgrade dependencies in need:

```bash
$ lein ancient upgrade :interactive

[com.taoensso/timbre "2.4.1"] is available but we use "2.1.2"
Do you want to upgrade? [yes/no] yes

[potemkin "0.3.1"] is available but we use "0.3.0"
Do you want to upgrade? [yes/no] no

[pandect "0.3.0"] is available but we use "0.2.3"
Do you want to upgrade? [yes/no] yes

2 artifacts upgraded.
```

Omit `:interactive` if lein-ancient should just do its thing; use `:print` for a dry-run, 
printing out the resulting file instead of writing back to disk.

To see all available options, call:

```
lein help ancient
```

## Supported Repository Types

- HTTP/HTTPS Repositories
- Local Repositories
- [Amazon S3 Repositories](https://github.com/technomancy/s3-wagon-private) (private)

## License

Copyright &copy; 2013 Yannick Scherer

Distributed under the Eclipse Public License, the same as Clojure.

_lein-outdated_ Source Copyright &copy; Alex Osborne &amp; Contributors
