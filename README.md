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
[lein-ancient "0.5.0-RC1"]      ;; unstable
```

This plugin is destined for Leiningen >= 2.0.0.

## Check Artifacts

`lein-ancients` default behaviour is to check your current project (or a given file/directory) for
artifacts that have newer versions available, e.g.:

```bash
$ lein ancient
[com.taoensso/timbre "2.6.2"] is available but we use "2.1.2"
[potemkin "0.3.3"] is available but we use "0.3.0"
[pandect "0.3.0"] is available but we use "0.2.3"
```

You can specify the type of versions to check with `:allow-snapshots`, `:allow-qualified` and 
`:allow-all`, and the kind of artifacts with `:plugins` and `:all`:

```bash
$ lein ancient :allow-snapshots
[com.taoensso/timbre "2.6.2"] is available but we use "2.1.2"
[potemkin "0.3.4-SNAPSHOT"] is available but we use "0.3.0"
[pandect "0.3.0"] is available but we use "0.2.3"
[midje "1.6-SNAPSHOT"] is available but we use "1.5.1"

$ lein ancient :plugins
[lein-midje "3.1.2"] is available but we use "3.0.1"
```

It works recursively, too:

```bash
$ lein ancient :recursive
-- ./panoptic/project.clj
[com.taoensso/timbre "2.6.2"] is available but we use "2.1.2"
[potemkin "0.3.3"] is available but we use "0.3.0"
[pandect "0.3.0"] is available but we use "0.2.3"

-- ./rewrite-clj/project.clj
[org.clojure/tools.reader "0.7.8"] is available but we use "0.7.5"
[potemkin "0.3.3"] is available but we use "0.3.2"
```

To let `lein-ancient` perform the same checks for the profiles in `~/.lein/profiles.clj`, run
it using:

```bash
$ lein ancient profiles [<options>]
...
```

## Upgrade Artifacts

`lein-ancient` lets you upgrade artifacts automatically and interactively, accepting
the same options as the default and `profiles` tasks:

```bash
$ lein ancient upgrade :interactive

[com.taoensso/timbre "2.6.2"] is available but we use "2.1.2"
Do you want to upgrade? [yes/no] yes

[potemkin "0.3.3"] is available but we use "0.3.0"
Do you want to upgrade? [yes/no] no

[pandect "0.3.0"] is available but we use "0.2.3"
Do you want to upgrade? [yes/no] yes

2 artifacts were upgraded.
```

Omit `:interactive` if lein-ancient should just do its thing; use `:print` for a dry-run, 
printing out the resulting file instead of writing back to disk. You can even perform a
recursive upgrade run by supplying `:recursive`.

You can upgrade the global user profiles by running:

```bash
$ lein ancient upgrade-profiles [<options>]
...
```

## Regression Testing

You'd want to make sure that the upgraded dependencies don't mess with your library or application,
wouldn't you? Unit tests are your friend and lein-ancient offers a mechanism to automatically run
them after an upgrade - and revert to the original state if they fail. Simply create an alias
`test-ancient` in your `project.clj`:

```clojure
...
  :aliases {"test-ancient" ["with-profile" "..." "midje"]}
...
```

(Note that referencing other aliases does not work yet.)

## Latest library vector

`lein-ancient` lets you check for the latest available version of a
library and gives you the exact vector to paste into your new
project.clj

```bash
$ lein ancient latest com.taoensso/timbre
[com.taoensso/timbre "2.6.2"]
```

This can be used programatically from editors to automatically insert dependency vectors

## Supported Repository Types

- HTTP/HTTPS Repositories
- Local Repositories
- [Amazon S3 Repositories](https://github.com/technomancy/s3-wagon-private) (private)

## License

Copyright &copy; 2013 Yannick Scherer

Distributed under the Eclipse Public License, the same as Clojure.

_lein-outdated_ Source Copyright &copy; Alex Osborne &amp; Contributors
