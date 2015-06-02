# Acceptance/Integration tests for Build Info

A project to test the build-info extractors such as Maven/Gradle/Ivy and Generic.
Basically the main goal is to cover the use of the build-info together with the build tools as an end user, for example the end user can be also the CI server Artifactory plugin.

## Getting Started

### Setup Artifactory
There are two ways to setup Artifactory for the integration tests:
* Under conf/config.groovy file.
* Set **environment variables** or **system properties**.

Available Project Properties:
* ARTIFACTORY_URL -         Artifactory address [Madatory]
* ARTIFACTORY_USER -        Artifactory user [Madatory]
* ARTIFACTORY_PASSWORD -    Artifactory user password [Madatory]
* TEST_CONFIGURATION_PATH - Path to the test configuration file

### Setup Docker
Basically this project interact with Docker via his Remote API.
So the only prerequisit is to set the following **environment variables** or **system properties**:

DOCKER_HTTP_HOST - http url to the docker server host.

For example:
DOCKER_HTTP_HOST="http://boot2docker:2375/"

## Running Tests

Run the command: `gradlew test`

## Writing Tests

### Build Process suite tests
In order to add test spec for this suite:
* Add the spec to the abstract class `BuildTestBaseSpec` under `@Suite.SuiteClasses`.
* The spec needs to extends `Specification` (spock).
* The spec needs to include a list member with annotation `@LabelMatch`, that includes the label names that match the labels closure in the test config files. 
 
#### @LabelMatch
Use this extension only on List object.
Use this extension in order to populate the relevant `TestProfile` to your list member
in the Spec.
The population logic is by matching labels from the test config file to the specific Spec.
  Example:

  For the following labels under the test config file:

  `labels=['artifacts']`
  
  You need to add the following annotation:
  
  `@LabelMatch(['artifacts'])`

### Run your test with Docker container
In case of running the suite tests with Docker container, such as Artifactory: 
* Add the `@RunWithDocker` on your base test class
* Make sure your base test class `extends AbstractJUnitTest`, this abstract class is responsible to execute custom annotations such as **RunWithDocker**. For example the class `BuildTestBaseSpec`.

##### @RunWithDocker
There are two ways to use this annotation:
* Give `registry` and `repo` input in order to fetch the image from docker registry
* Give docker file path (`dockerFilePath`) in order to build the image from it
 
Either way image ID (`imageId`) is mandatory, `containerPort` is the port that is exposed from the container, `hostPort` is the port that is mapped from the container to the host.



