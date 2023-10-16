#!/usr/bin/env bash

set -eufo pipefail
# We want to see what's going on
set -x

# The ruby version may have been set by the CI runner. Stash
# changes while we check to see if we need to reformat the
# code.
git config user.email "selenium@example.com"
git config user.name "CI Build"
git commit -am 'Temp commit to allow format to run cleanly'

# Fail the build if the format script needs to be re-run
./scripts/format.sh
git diff --exit-code

# Now we're made it out, reapply changes made by the build
# runner
git reset --soft HEAD^

# The NPM repository rule wants to write to the HOME directory
# but that's configured for the remote build machines, so run
# that repository rule first so that the subsequent remote
# build runs successfully. We don't care what the output is.
bazel query @npm//:all >/dev/null

# Now run the tests. The engflow build uses pinned browsers
# so this should be fine
# shellcheck disable=SC2046
bazel test --config=remote-ci --build_tests_only --test_tag_filters=-exclusive-if-local,-skip-remote --keep_going --flaky_test_attempts=2  //java/... //py/... -- $(cat .skipped-tests | tr '\n' ' ')
# Build the entire java tree
bazel build --config=remote-ci java/src/... //py:selenium-wheel
