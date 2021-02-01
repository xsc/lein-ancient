# lein-ancient

![CI](https://github.com/xsc/lein-ancient/workflows/CI/badge.svg?branch=master)
[![Clojars Artifact](https://img.shields.io/clojars/v/lein-ancient.svg)](https://clojars.org/lein-ancient)

A [Leiningen][lein] plugin to check your project for outdated dependencies and
plugins, as well as upgrade them if desired.

This plugin supersedes [lein-outdated][lein-outdated] and uses metadata XML
files in the different Maven repositories instead of a [Lucene][lucene]-based
search index. Version comparison is done using [version-clj][version-clj].

[lein]: https://github.com/technomancy/leiningen
[lein-outdated]: https://github.com/ato/lein-outdated
[lucene]: http://lucene.apache.org/core/
[version-clj]: https://github.com/xsc/version-clj

lein-ancient is destined for Leiningen >= 2.4.0.

## Usage

Install `lein-ancient` by putting the following into the `:plugins` vector of
the `:user` profile in your `~/.lein/profiles.clj`:

```clojure
[lein-ancient "0.7.0"]
```

Once `lein-ancient` is installed, use Leiningen's built-in help feature to
see how to use it:

``` bash
lein help ancient
lein help ancient <subtask>
```

### Check Artifacts

`lein-ancient`'s default behaviour is to check your current project (or a given
file/directory) for artifacts that have newer versions available, e.g.:

```bash
$ lein ancient
[com.taoensso/timbre "2.6.2"] is available but we use "2.1.2"
[potemkin "0.3.3"] is available but we use "0.3.0"
[pandect "0.3.0"] is available but we use "0.2.3"
```

You can specify the type of versions to check with `:allow-snapshots`,
`:allow-qualified` and `:allow-all`, and the kind of artifacts with `:plugins`,
`:java-agents`, and `:all`:

```bash
$ lein ancient :allow-snapshots
[com.taoensso/timbre "2.6.2"] is available but we use "2.1.2"
[potemkin "0.3.4-SNAPSHOT"] is available but we use "0.3.0"
[pandect "0.3.0"] is available but we use "0.2.3"
[midje "1.6-SNAPSHOT"] is available but we use "1.5.1"

$ lein ancient :plugins
[lein-midje "3.1.2"] is available but we use "3.0.1"

$ lein ancient :java-agents
[com.newrelic.agent.java/newrelic-agent "3.43.0"] is available but we use "3.35.1"
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

To let `lein-ancient` perform the same checks for the profiles in
`~/.lein/profiles.clj`, run it using:

```bash
$ lein ancient check-profiles [<options>]
...
```

### Upgrade Artifacts

`lein-ancient` lets you upgrade artifacts automatically and interactively,
accepting the same options as the default and `profiles` tasks:

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

Omit `:interactive` if lein-ancient should just do its thing; use `:print` for a
dry-run, printing out the resulting file instead of writing back to disk. You
can even perform a recursive upgrade run by supplying `:recursive`.

You can upgrade the global user profiles by running:

```bash
$ lein ancient upgrade-profiles [<options>]
...
```

### Exclusion/Selection of Artifacts

It is possible to add a marker to an artifact vector using the `:upgrade` key:

```clojure
:dependencies [[org.clojure/clojure "1.7.0"]
               [pandect "0.4.0" :upgrade :crypto]]
```

This can be :

- the boolean value `false`: do never check/upgrade this artifact,
- a keyword: mark the artifact with the given keyword,
- a vector of keywords: mark the artifact with the given keywords.

By supplying `:only` or `:exclude` on the command line, you can run
checks/upgrades on only those artifacts that match the given markers:

```
$ lein ancient upgrade :exclude crypto
```

There are two predefined markers that get attached to matching artifacts:
`snapshots` to SNAPSHOT versions and `qualified` to qualified (i.e.
alpha/beta/...) ones.

 __Note__ that `:only`/`:exclude` compose with the
`:dependencies`/`:plugins`/`:all` options - to e.g. only upgrade SNAPSHOTs of
plugins you'd use:

```
$ lein ancient upgrade :plugins :only snapshots
```

### Regression Testing

You'd want to make sure that the upgraded dependencies don't mess with your
library or application, wouldn't you? Unit tests are your friend and
lein-ancient offers a mechanism to automatically run them after an upgrade - and
revert to the original state if they fail. By default, `lein test` is used for
testing; if you want a specific command to be run simply create an alias `test`
in your `project.clj`:

```clojure
...
  :aliases {"test" ["with-profile" "..." "midje"]}
...
```

(Note that referencing other aliases does not work yet.)

### Inspect Artifact Versions

If you do not have access to a browser or are just too lazy/annoyed to leave the
command line, the tasks `get` and `latest` might be just the thing for you! The
former prints some human-readable artifact data to your console while the latter
only retrieves the artifact vector, e.g. destined for your `project.clj`.

```bash
$ lein ancient show-versions com.taoensso/timbre :allow-all
Getting Version Information for com.taoensso/timbre from 2 Repositories ...
  * 39 version(s) found.
  * latest release:          "2.6.2"
  * latest SNAPSHOT:         "2.0.0-SNAPSHOT"
  * latest qualified:        "1.4.0-alpha1"
  * all releases:            [ "2.6.2" "2.6.1" "2.6.0" "2.5.0" "2.4.1" ...
...

$ lein ancient show-latest com.taoensso/timbre :allow-all
[com.taoensso/timbre "2.6.2"]
```

If you're using Emacs you can access this functionality using Adam Clement's
[latest-clojure-libraries](https://github.com/AdamClements/latest-clojure-libraries)
plugin without leaving your buffer.

## Supported Repository Types

- HTTP/HTTPS Repositories
- Local Repositories
- [Amazon S3 Repositories](https://github.com/technomancy/s3-wagon-private) (private)

## License

```
MIT License

Copyright (c) 2013-2020 Yannick Scherer

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

### Derivative

_lein-outdated_ Source Copyright &copy; Alex Osborne &amp; Contributors
