#!/usr/bin/env bash
# Runs the build-info snapshot flow: configures the JFrog CLI, runs a JFrog
# audit, deletes former snapshot artifacts, builds and publishes to
# Artifactory, publishes build-info, and distributes the snapshot release
# bundle.
#
# Runnable both from GitHub Actions (snapshot.yml) and directly on a
# developer's machine - export the env vars below and run this script from
# the repository root.
#
# Expected environment variables:
#   ARTIFACTORY_URL
#   ARTIFACTORY_USER
#   ARTIFACTORY_APIKEY
#   RUN_NUMBER  - unique build/run number used to tag the snapshot bundle (e.g. GitHub Actions run_number)
set -euo pipefail

jf c rm --quiet
jf c add internal --url="$ARTIFACTORY_URL" --user="$ARTIFACTORY_USER" --password="$ARTIFACTORY_APIKEY"
jf gradlec --use-wrapper --uses-plugin --repo-resolve ecosys-maven-remote --repo-deploy ecosys-oss-snapshot-local

jf audit --fail=false

# Delete former snapshots to make sure the release bundle will not contain stale artifacts
jf rt del "ecosys-oss-snapshot-local/org/jfrog/buildinfo/*" --quiet

jf gradle clean aP -x test

jf rt bag && jf rt bce
jf rt bp

jf ds rbc ecosystem-build-info-snapshot "$RUN_NUMBER" --spec=./release/specs/dev-rbc-filespec.json --sign
jf ds rbd ecosystem-build-info-snapshot "$RUN_NUMBER" --site="releases.jfrog.io" --sync
