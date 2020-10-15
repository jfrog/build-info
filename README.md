[![Build status](https://ci.appveyor.com/api/projects/status/omscno1vb7g11qu2?svg=true)](https://ci.appveyor.com/project/jfrog-ecosystem/build-info)

## Overview

Build Info is Artifactory's open integration layer for the CI servers and build tools. The build information is sent to Artifactory in json format.

## Building and testing the sources

* The code is built using Gradle and includes integration tests.<br/>
* It must run using JDK 8 and Gradle 5.6.2. If you are using different gradle version you can use the provided gradle wrapper.<br/>
* In order to run tests the following environment variables must be set:
```
export BITESTS_ARTIFACTORY_URL='http://localhost:8081/artifactory'
export BITESTS_ARTIFACTORY_USERNAME=admin
export BITESTS_ARTIFACTORY_PASSWORD=password
export BITESTS_ARTIFACTORY_PIP_ENV=/Users/user/venv-test/bin
```
* Before running the tests, please make sure you have a generic repository named *tests* in Artifactory.

### Building
When running the following commands to build the code, the entire testing suite is also executed. See the *Testing* section for configuration instructions prior to running the tests.

To build the code using the gradle wrapper in Unix run:
```
> ./gradlew clean build
```
To build the code using the gradle wrapper in Windows run:
```
> gradlew clean build
```
To build the code using the environment gradle run:
```
> gradle clean build
```
To build the code without running the tests, add to the "clean build" command the "-x test" option, for example:
```
> ./gradlew clean build -x test
```

### Testing
To run *all* tests:
```
> ./gradlew clean test
```

#### Extractor tests
```
> ./gradlew clean build-info-api:test build-info-client:test build-info-extractor:test build-info-vcs:test
```

#### Maven tests
```
> ./gradlew clean build-info-extractor-maven3:test
```

#### Gradle tests
```
> ./gradlew clean build-info-extractor-gradle:test
```

#### Npm tests
* Add npm executable to the system search path (PATH environment variable).
```
> ./gradlew clean build-info-extractor-npm:test
```

#### Go tests
* Add Go executable to the system search path (PATH environment variable).
```
> ./gradlew clean build-info-extractor-go:test
```

#### Pip tests
* Add Python and pip executables to the system search path (PATH environment variable).
* Pip tests must run inside a clean pip-environment. Create a virtual environment and provide its path using the 'BITESTS_ARTIFACTORY_PIP_ENV' variable.
When running on a Windows machine, provide the path to the 'Scripts' directory.
When running on a unix machine, provide the path to the 'bin' directory.
```
> python -m venv buildinfo-tests-env
> export BITESTS_ARTIFACTORY_PIP_ENV=/Users/user/buildinfo-tests-env/bin
> ./gradlew clean build-info-extractor-pip:test
```

#### NuGet tests
* Add Nuget & Dotnet executable to the system search path (PATH environment variable).
```
> ./gradlew clean build-info-extractor-nuget:test
```

#### Docker tests
* Docker tests run only on Linux/mac.
* In addition to the general environment variables required for running the tests, you must set the following environment variables, required for the docker tests:

 ```
  export BITESTS_ARTIFACTORY_DOCKER_DOMAIN='server-build-info-tests-docker.jfrog.io/'
  export BITESTS_ARTIFACTORY_DOCKER_REPO=build-info-tests-docker
  export BITESTS_ARTIFACTORY_DOCKER_HOST=tcp://127.0.0.1:1234
 ```
 * For OSX agents, run a Socat container:
 ```
 docker run -d -v /var/run/docker.sock:/var/run/docker.sock -p 127.0.0.1:1234:1234 bobrik/socat TCP-LISTEN:1234,fork UNIX-CONNECT:/var/run/docker.sock
 ```
 ```
> ./gradlew clean build-info-extractor-docker:test
```

* Before running the tests, please make sure you have a local docker repository named *build-info-tests-docker* in Artifactory.

###  Testing on Artifactory OSS
When testing with an instance of Artifactory OSS, only supported tests are for the build-info-gradle-extractor.

On Artifactory pro, the tests infrastructure will create the test repositories by REST API.
To run the tests on Artifactory OSS, you should create the Gradle repositories by yourself.
To run Gradle tests on Artifactory OSS:
* Start Artifactory on docker container:
```
> docker run --name artifactory -d -p 8081:8081 -p 8082:8082 docker.bintray.io/jfrog/artifactory-oss:latest
```
* With your web browser, go to Artifactory UI: http://127.0.0.1:8081/artifactory
* Create 3 Gradle repositories:
  * Local repository: `build-info-tests-gradle-local`
  * Remote repository to jcenter: `build-info-tests-gradle-remote`
  * Virtual repository containing both the remote and local: `build-info-tests-gradle-virtual`
* Run tests `./gradlew build-info-extractor-gradle:test`

## More about build-info
Read more about build-info in the [Build Integration documentation](https://www.jfrog.com/confluence/display/JFROG/Build+Integration).