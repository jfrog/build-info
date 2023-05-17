# Build-Info

[![Build Info Extractor plugin](https://img.shields.io/maven-central/v/org.jfrog.buildinfo/build-info-extractor?label=build-info-extractor&style=for-the-badge)](https://search.maven.org/artifact/org.jfrog.buildinfo/build-info-extractor) [![Gradle plugin](https://img.shields.io/gradle-plugin-portal/v/com.jfrog.artifactory?label=Gradle%20Artifactory%20plugin&style=for-the-badge)](https://plugins.gradle.org/plugin/com.jfrog.artifactory)

[![Scanned by Frogbot](https://raw.github.com/jfrog/frogbot/master/images/frogbot-badge.svg)](https://github.com/jfrog/frogbot#readme) [![Tests](https://github.com/jfrog/build-info/actions/workflows/integrationTests.yml/badge.svg)](https://github.com/jfrog/build-info/actions/workflows/integrationTests.yml) 

## Overview

Build Info is Artifactory's open integration layer for the CI servers and build tools. The build information is sent to Artifactory in json format.

## Building and testing the sources

* The code is built using Gradle and includes integration tests.<br/>
* It must run using JDK 8 and Gradle 5.6.2. If you are using different gradle version you can use the provided gradle wrapper.<br/>
* In order to run tests the following environment variables must be set:
```bash
export BITESTS_PLATFORM_URL='http://localhost:8081'
export BITESTS_PLATFORM_USERNAME=admin
export BITESTS_PLATFORM_ADMIN_TOKEN=admin-access-token
export BITESTS_ARTIFACTORY_PIP_ENV=/Users/user/venv-test/bin
```
See [here](https://www.jfrog.com/confluence/display/JFROG/Access+Tokens#AccessTokens-GeneratingAdminTokens) how to generate an admin token for the above environment variable.

### Building
When running the following commands to build the code, the entire testing suite is also executed. See the *Testing* section for configuration instructions prior to running the tests.

To build the code using the gradle wrapper in Unix run:
```bash
./gradlew clean build
```
To build the code using the gradle wrapper in Windows run:
```bash
gradlew clean build
```
To build the code using the environment gradle run:
```bash
gradle clean build
```
To build the code without running the tests, add to the "clean build" command the "-x test" option, for example:
```bash
./gradlew clean build -x test
```

### Testing
To run *all* tests:
```bash
./gradlew clean test
```

#### Extractor tests
```bash
./gradlew clean build-info-api:test build-info-client:test build-info-extractor:test build-info-vcs:test
```

#### Maven tests
```bash
./gradlew clean build-info-extractor-maven3:test
```

#### Gradle tests
```bash
./gradlew clean build-info-extractor-gradle:test
```

#### npm tests
* Add npm executable to the system search path (PATH environment variable).
* npm 7.7 or above is required.
```bash
./gradlew clean build-info-extractor-npm:test
```

#### Go tests
* Add Go executable to the system search path (PATH environment variable).
* Go v1.14 or above is required.
```bash
./gradlew clean build-info-extractor-go:test
```

#### Pip tests
* Add Python and pip executables to the system search path (PATH environment variable).
* Pip tests must run inside a clean pip-environment. Create a virtual environment and provide its path using the 'BITESTS_ARTIFACTORY_PIP_ENV' variable.
When running on a Windows machine, provide the path to the 'Scripts' directory.
When running on a unix machine, provide the path to the 'bin' directory.
```bash
python -m venv buildinfo-tests-env
export BITESTS_ARTIFACTORY_PIP_ENV=/Users/user/buildinfo-tests-env/bin
./gradlew clean build-info-extractor-pip:test
```

#### NuGet tests
* Add Nuget & Dotnet executable to the system search path (PATH environment variable).
```bash
./gradlew clean build-info-extractor-nuget:test
```

#### Docker tests
* Docker tests run only on Linux/mac.
* In addition to the general environment variables required for running the tests, you must set the following environment variables, required for the docker tests:
  ```bash
  export BITESTS_PLATFORM_CONTAINER_REGISTRY=127.0.0.1:8081
  export BITESTS_ARTIFACTORY_DOCKER_HOST=tcp://127.0.0.1:1234
  ```
* For OSX agents, run a Socat container:
 ```bash
 docker run -d -v /var/run/docker.sock:/var/run/docker.sock -p 127.0.0.1:1234:1234 bobrik/socat TCP-LISTEN:1234,fork UNIX-CONNECT:/var/run/docker.sock
 ```
* Run tests:
 ```bash
./gradlew clean build-info-extractor-docker:test
```

## More about build-info
Read more about build-info in the [Build Integration documentation](https://www.jfrog.com/confluence/display/JFROG/Build+Integration).

## Release notes
The release notes are available [here](https://github.com/jfrog/build-info/releases).
