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
    local CURRENT_VERSION=$(head -n 1 "project.clj" | cut -c33- | sed 's/"//g')
    sed -i'.bak' "s/ancient-clj \".*\"/ancient-clj \"$CURRENT_VERSION\"/" "lein-ancient/project.clj"
    rm lein-ancient/project.clj.bak
}

function bump_version() {
    $LEIN $LEIN_BUMP "$@"
}

function commit_release() {
    $LEIN do vcs commit, vcs tag "v"
}

function commit_snapshot() {
    $LEIN do vcs commit
}

function deploy_artifacts() {
    cd ancient-clj
    $LEIN deploy clojars
    cd ../lein-ancient
    $LEIN deploy clojars
    cd ..
}

function run_tests() {
    cd ancient-clj
    $LEIN test
    $LEIN install > /dev/null
    cd ../lein-ancient
    $LEIN test
    cd ..
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
