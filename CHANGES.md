# CHANGES: `lein-ancient`

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
