# CHANGES: `lein-ancient`

## 0.4.4

- Fix for Amazon S3 Repositories (missing credentials)

## 0.4.3

- Using [ancient-clj](https://github.com/xsc/ancient-clj) as backend.
- New modes `:upgrade` and `:upgrade-global` for project.clj/profiles.clj rewriting
  (based on [rewrite-clj](https://github.com/xsc/rewrite-clj))

## 0.4.2

- Added `:get` mode to retrieve version data for a specific package.

## 0.4.1

- Added more verbose messages (indicating URLs of metadata files and amount of downloaded data).
- Added `:aggressive` mode (always checks all available repositories and does not stop after first
  artifact match).
- Added Amazon S3 retriever.
- Restructured retriever code.

## 0.4.0

- Switched to [version-clj](https://github.com/xsc/version-clj) for version comparison.

## 0.3.7

- Implemented more generic version comparison (see [here](http://docs.codehaus.org/display/MAVEN/Versioning)).

## 0.3.6

- Bugfix to remove infinite loop.

## 0.3.5

- Bugfix in version comparison when using different lengths ("1.0.1" was equal to "1.0" because
  the latter was not correctly extended to "1.0.0").

## 0.3.4 

- Modified version comparison algorithm in response to issues encountered when resolving #5.
- Added verbose message indicating XML parse error.
- Fixed issue with `:allow-snapshots` that made it only usable with `:allow-qualified`.

## 0.3.3

- Using additional "maven-metadata-local.xml" when resolving dependency in local repositories
  (see issue #5).

## 0.3.2

- Unknown repository formats should not break dependency resolution (see issue #5).

## 0.3.1

- Fixed duplicate artifact handling. 

## 0.3.0

- Removed `tools.cli` from Dependencies.
- Added `colorize` to Dependencies.
- Colorizing output.
- Added `:verbose` (print progress messages), `:check-clojure` (do not exclude Clojure from version check) 
  and `:no-colors` (disable colorized output) flags.
- Changed output to omit group ID if artifact and group ID are identical.
- Generalized metadata retrieval logic to be able to easily add further repository types (e.g. S3).
- Rearranged/Split code.

## <= 0.2.0

Initial Releases.
