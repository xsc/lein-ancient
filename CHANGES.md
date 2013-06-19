# CHANGES: `lein-ancient`

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
