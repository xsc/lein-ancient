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
$ lein ancient :upgrade :interactive

[com.taoensso/timbre "2.4.1"] is available but we use "2.1.2"
Do you want to upgrade? [yes/no] yes
Upgrade to [com.taoensso/timbre "2.4.1"] from "2.1.2"

[potemkin "0.3.1"] is available but we use "0.3.0"
Do you want to upgrade? [yes/no] no

[pandect "0.3.0"] is available but we use "0.2.3"
Do you want to upgrade? [yes/no] yes
Upgrade to [pandect "0.3.0"] from "0.2.3"
```

Omit `:interactive` if lein-ancient should just do its thing; use `:print` for a dry-run, 
printing out the resulting file instead of writing back to disk.

To see all available options, call `lein help ancient`:

```bash
$ lein help ancient
Check your Projects for outdated Dependencies. 
  
   Usage:

     lein ancient [<options>]
     lein ancient :get <package> [<options>]
     lein ancient :upgrade [<options>]
     lein ancient :upgrade-global [<options>]

   Modes:

     :get                 Retrieve artifact information from Maven repositories.
     :upgrade             Replace artifacts in your 'project.clj' with newer versions.
     :upgrade-global      Replace plugins in '~/.lein/profiles.clj' with newer versions.

   Commandline Options:
  
     :all                 Check Dependencies and Plugins.
     :dependencies        Check Dependencies. (default)
     :plugins             Check Plugins.
     :no-profiles         Do not check Dependencies/Plugins in Profiles.
     :allow-qualified     Allow '*-alpha*' versions & co. to be reported as new.
     :allow-snapshots     Allow '*-SNAPSHOT' versions to be reported as new.
     :check-clojure       Include Clojure (org.clojure/clojure) in checks.
     :aggressive          Check all available repositories (= Do not stop after first artifact match).
     :interactive         Run ':upgrade' in interactive mode, prompting whether to apply changes.
     :print               Print result of ':upgrade' task instead of writing it to 'project.clj'.
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
