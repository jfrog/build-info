[![Build status](https://ci.appveyor.com/api/projects/status/omscno1vb7g11qu2?svg=true)](https://ci.appveyor.com/project/jfrog-ecosystem/build-info)

## Overview

Build Info is Artifactory's open integration layer for the CI servers and build tools. The build information is sent to Artifactory in json format.

## Building and testing the sources

* The code is built using Gradle and includes integration tests.<br/>
* It must run using JDK 8 and Gradle 5.6.2. If you are using different gradle version you can use the provided gradle wrapper.<br/>
* In order to run tests the following environment variables must be set:
```bash
export BITESTS_PLATFORM_URL='http://localhost:8081'
export BITESTS_PLATFORM_USERNAME=admin
export BITESTS_PLATFORM_PASSWORD=password
export BITESTS_ARTIFACTORY_PIP_ENV=/Users/user/venv-test/bin
```
Before running the tests:
1. Create a generic repository named *tests* in Artifactory.
2. Create a project with *btests* as its key in Artifactory.

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
* Create the following docker repositories in Artifactory:
  * docker-local
  * docker-remote
  * docker-virtual (contains both docker-local & docker-remote repositories)
* In addition to the general environment variables required for running the tests, you must set the following environment variables, required for the docker tests:
  * Replace `localhost:8081` prefix with your docker registry domain if needed.
 ```bash
  export BITESTS_ARTIFACTORY_DOCKER_LOCAL_DOMAIN=localhost:8081/docker-local
  export BITESTS_ARTIFACTORY_DOCKER_REMOTE_DOMAIN=localhost:8081/docker-remote
  export BITESTS_ARTIFACTORY_DOCKER_VIRTUAL_DOMAIN=localhost:8081/docker-virtual
  export BITESTS_ARTIFACTORY_DOCKER_LOCAL_REPO=docker-local
  export BITESTS_ARTIFACTORY_DOCKER_REMOTE_REPO=docker-remote
  export BITESTS_ARTIFACTORY_DOCKER_VIRTUAL_REPO=docker-virtual
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

###  Testing on Artifactory OSS
When testing with an instance of Artifactory OSS, only supported tests are for the build-info-gradle-extractor.

On Artifactory pro, the tests infrastructure will create the test repositories by REST API.
To run the tests on Artifactory OSS, you should create the Gradle repositories by yourself.
To run Gradle tests on Artifactory OSS:
* Start Artifactory on docker container:
```bash
docker run --name artifactory -d -p 8081:8081 -p 8082:8082 releases-docker.jfrog.io/jfrog/artifactory-oss:latest
```
* With your web browser, go to Artifactory UI: http://127.0.0.1:8081/artifactory
* Create 3 Gradle repositories:
  * Local repository: `build-info-tests-gradle-local`
  * Remote repository to jcenter: `build-info-tests-gradle-remote`
  * Virtual repository containing both the remote and local: `build-info-tests-gradle-virtual`
* Run tests `./gradlew build-info-extractor-gradle:test`

## More about build-info
Read more about build-info in the [Build Integration documentation](https://www.jfrog.com/confluence/display/JFROG/Build+Integration).

## Release notes
The release notes are available [here](RELEASE.md).
