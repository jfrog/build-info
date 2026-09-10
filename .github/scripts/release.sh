#!/usr/bin/env bash
# Runs the build-info release flow: configures git identity and the JFrog CLI,
# optionally runs a JFrog audit, bumps the release version and tags it, builds
# and publishes to Artifactory, publishes build-info, distributes the release
# bundle, publishes to Maven Central, and bumps the next development version.
#
# Runnable both from GitHub Actions (release.yml) and directly on a
# developer's machine - export the env vars below and run this script from
# the repository root, on the branch/checkout you want to release from.
#
# Expected environment variables:
#   NEXT_VERSION                     - e.g. 2.44.0
#   NEXT_DEVELOPMENT_VERSION         - e.g. 2.44.x-SNAPSHOT
#   NEXT_GRADLE_VERSION              - e.g. 4.36.0
#   NEXT_GRADLE_DEVELOPMENT_VERSION  - e.g. 4.36.x-SNAPSHOT
#   ARTIFACTORY_URL
#   ARTIFACTORY_USER
#   ARTIFACTORY_APIKEY
#   MVN_CENTRAL_USER
#   MVN_CENTRAL_PASSWORD
#   MVN_CENTRAL_SIGNING_KEY          - base64-encoded GPG signing key
#   MVN_CENTRAL_SIGNING_PASSWORD
#   SKIP_AUDIT_CHECK                 - optional, "true" to skip the jf audit step (default: "false")
set -euo pipefail

git config user.name "JFrog CI"
git config user.email "eco-system@jfrog.com"

jf c rm --quiet
jf c add internal --url="$ARTIFACTORY_URL" --access-token="$ARTIFACTORY_APIKEY"
jf gradlec --use-wrapper --uses-plugin --repo-resolve ecosys-maven-remote --repo-deploy ecosys-oss-release-local

if [[ "${SKIP_AUDIT_CHECK:-false}" != "true" ]]; then
  jf audit --fail=false
fi

sed -i -e "/build-info-version=/ s/=.*/=$NEXT_VERSION/" -e "/build-info-extractor-gradle-version=/ s/=.*/=$NEXT_GRADLE_VERSION/" gradle.properties
git commit -am "[artifactory-release] Release version ${NEXT_VERSION} [skipRun]" --allow-empty
git tag build-info-extractor-${NEXT_VERSION}
git tag build-info-gradle-extractor-${NEXT_GRADLE_VERSION}
git push
git push --tags

export ORG_GRADLE_PROJECT_signingKey=$(echo "$MVN_CENTRAL_SIGNING_KEY" | base64 -d)
export ORG_GRADLE_PROJECT_signingPassword="$MVN_CENTRAL_SIGNING_PASSWORD"
jf gradle clean aP -x test -Psign

jf rt bag && jf rt bce
jf rt bp

jf ds rbc ecosystem-build-info $NEXT_VERSION --spec=./release/specs/prod-rbc-filespec.json --spec-vars="version=$NEXT_VERSION;gradleVersion=$NEXT_GRADLE_VERSION" --sign
jf ds rbd ecosystem-build-info $NEXT_VERSION --site="releases.jfrog.io" --sync

export ORG_GRADLE_PROJECT_sonatypeUsername="$MVN_CENTRAL_USER"
export ORG_GRADLE_PROJECT_sonatypePassword="$MVN_CENTRAL_PASSWORD"
export ORG_GRADLE_PROJECT_signingKey=$(echo "$MVN_CENTRAL_SIGNING_KEY" | base64 -d)
export ORG_GRADLE_PROJECT_signingPassword="$MVN_CENTRAL_SIGNING_PASSWORD"
./gradlew clean build publishToSonatype closeAndReleaseSonatypeStagingRepository -x test -Psign

sed -i -e "/build-info-version=/ s/=.*/=$NEXT_DEVELOPMENT_VERSION/" -e "/build-info-extractor-gradle-version=/ s/=.*/=$NEXT_GRADLE_DEVELOPMENT_VERSION/" gradle.properties
git commit -am "[artifactory-release] Next development version [skipRun]"
git push
