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

Put the following into the `:plugins` vector of the `:user` profile in your `~/.lein/profiles.clj`:

```clojure
[lein-ancient "0.4.3"]
```

This plugin is destined for Leiningen >= 2.0.0.

__Command Line__

You can use `lein-ancient` to check the dependencies/plugins of a project and those specified
in `~/.lein/profiles.clj`.

```bash
$ lein ancient
[com.taoensso/timbre "2.1.2"] is available but we use "2.0.1"

$ lein ancient :allow-qualified
[com.taoensso/timbre "2.1.2"] is available but we use "2.0.1"
[midje "1.6-alpha2"] is available but we use "1.5.1"

$ lein ancient :allow-snapshots
[com.taoensso/timbre "2.1.2"] is available but we use "2.0.1"
[midje "1.6-SNAPSHOT"] is available but we use "1.5.1"

$ lein ancient :plugins
[lein-tarsier/lein-tarsier "0.10.0"] is available but we use "0.9.4"

$ lein ancient :all
[com.taoensso/timbre "2.1.2"] is available but we use "2.0.1"
[lein-tarsier/lein-tarsier "0.10.0"] is available but we use "0.9.4"
```

To see available options, call `lein help ancient`:

```bash
$ lein help ancient
Check your Projects for outdated Dependencies. 

   Usage:

     lein ancient :get <package> [<options>]
     lein ancient [<options>]
   
   Commandline Options:
  
     :all                 Check Dependencies and Plugins.
     :dependencies        Check Dependencies. (default)
     :plugins             Check Plugins.
     :no-profiles         Do not check Dependencies/Plugins in Profiles.
     :allow-qualified     Allow '*-alpha*' versions & co. to be reported as new.
     :allow-snapshots     Allow '*-SNAPSHOT' versions to be reported as new.
     :check-clojure       Include Clojure (org.clojure/clojure) in checks.
     :aggressive          Check all available repositories (= Do not stop after first artifact match).
     :verbose             Produce progress indicating messages.
     :no-colors           Disable colorized output.

Arguments: ([& args])
```

## Supported Repository Types

- HTTP/HTTPS Repositories
- Local Repositories
- [Amazon S3 Repositories](https://github.com/technomancy/s3-wagon-private) (private)

## License

Copyright &copy; 2013 Yannick Scherer

Distributed under the Eclipse Public License, the same as Clojure.

_lein-outdated_ Source Copyright &copy; Alex Osborne &amp; Contributors
