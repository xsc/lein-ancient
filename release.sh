#!/bin/bash

# ---------------------------
# Data
LEIN="$(which lein) "
LEIN_EACH="$LEIN monolith each"
LEIN_BUMP="change version leiningen.release/bump-version"
LEVEL="$1"

set -eu
# ---------------------------
# Assertions
if [ ! -z "$(git status --porcelain)" ]; then
    echo "There are uncommitted changes or untracked files in the current repository." 1>&2
    exit 1
fi

if [ ! "master" = "$(git rev-parse --abbrev-ref HEAD)" ]; then
    echo "Can only release from master branch." 1>&2;
    exit 1;
fi

COMMITS_SINCE_LAST_RELEASE=$(git rev-list $(git rev-list --tags --no-walk --max-count=1 2> /dev/null)..HEAD --count 2> /dev/null)
if [[ "$COMMITS_SINCE_LAST_RELEASE" == "1" ]]; then
    echo "It seems there was only one commit since the last tag, indicating that no" 1>&2;
    echo "changes would be released." 1>&2;
    exit 1;
fi

# ---------------------------
# Helpers
function adjust_ancient_clj_version() {
    local CURRENT_VERSION=$(head -n 1 "ancient-clj/project.clj" | cut -c25- | sed 's/"//g')
    sed -i'.bak' "s/ancient-clj \".*\"/ancient-clj \"$CURRENT_VERSION\"/" "lein-ancient/project.clj"
    rm lein-ancient/project.clj.bak
}

function bump_version() {
    $LEIN_EACH :parallel 2 $LEIN_BUMP "$@"
}

function commit_release() {
    $LEIN_EACH :in ancient-clj do vcs commit, vcs tag "clojars-"
}

function commit_snapshot() {
    $LEIN_EACH :in ancient-clj do vcs commit
}

function deploy_artifacts() {
    $LEIN_EACH :in ancient-clj deploy clojars
    $LEIN_EACH :in lein-ancient isolated deploy clojars
}

function run_tests() {
    $LEIN_EACH :in ancient-clj install > /dev/null
    $LEIN_EACH test
}

# ---------------------------
# Release Flow
bump_version $LEVEL release
adjust_ancient_clj_version
run_tests
commit_release
deploy_artifacts
bump_version
commit_snapshot
git push origin master --tags
