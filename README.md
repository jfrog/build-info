## Overview

Build Info is Artifactory's open integration layer for the CI servers and build tools.
The build information is sent to Artifactory in json format.

## To build the project (with gradle 0.9.2 or above)
```console
> gradle -S build

or with the gradle wrapper in Unix

> ./gradlew -S build

and the gradle wrapper in Windows

> gradlew.bat -S build
```
## Build Info json format

```groovy
{
  "properties" : {
   /* Environment variables and properties collected from the CI server.
      The "buildInfo.env." prefix is added to environment variables and build related properties.
      For system variables there's no prefix. */
   "buildInfo.env.JAVA_HOME" : "",
   ...
  },
  "version" : "1.0.1", // Build Info schema version
  "name" : "My-build-name", // Build name
  "number" : "28", // Build number
  "type" : "MAVEN", // Build type (values currently supported: MAVEN, GRADLE, ANT, IVY and GENERIC)
  "buildAgent" : { // Build tool information
    "name" : "Maven", // Build tool type
    "version" : "3.0.5" // Build tool version
  },
  "agent" : { // CI Server information
    "name" : "Jenkins", // CI server type
    "version" : "1.554.2" // CI server version
  },
  "started" : "2014-09-30T12:00:19.893+0300", // Build start time in the format of yyyy-MM-dd'T'HH:mm:ss.SSSZ
  "artifactoryPluginVersion" : "2.3.1",
  "durationMillis" : 9762, // Build duration in milliseconds
  "artifactoryPrincipal" : "james", // Artifactory principal (the Artifactory user used for deployment)
  "url" : "http://my-ci-server/jenkins/job/My-project-name/28/", // CI server URL
  "vcsRevision" : "e4ab2e493afd369ae7bdc90d69c912e8346a3463", // VCS revision
  "vcsUrl" : "https://github.com/github-user/my-project.git", // VCS URL
  "licenseControl" : {	// Artifactory License Control information
    "runChecks" : true,	// Artifactory will run automatic license scanning after the build is complete (true/false)
    "includePublishedArtifacts" : true, // Should Artifactory run license checks on the build artifacts, in addition to the build dependecies (true/false) 
    "autoDiscover" : true, // Should Artifactory auto discover licenses (true/false)
    "scopesList" : "", // A space-separated list of dependency scopes/configurations to run license violation checks on. If left empty all dependencies from all scopes will be checked.
    "licenseViolationsRecipientsList" : "" // Emails of recipients that should be notified of license violations in the build info (space-separated list)
  },
  "buildRetention" : { // Build retention information
    "deleteBuildArtifacts" : true, // Automatically remove build artifacts stored in Artifactory (true/false)
    "count" : 100, // The maximum number of builds to store in Artifactory.
    "minimumBuildDate" : 1407345768020, // Earliest build date to store in Artifactory
    "buildNumbersNotToBeDiscarded" : [ ] // List of build numbers that should not be removed from Artifactory
  },
  /* List of build modules */	
  "modules" : [ { // The build's first module
    "properties" : { // Module properties
      "project.build.sourceEncoding" : "UTF-8"
    },
    "id" : "org.jfrog.test:multi2:4.2-SNAPSHOT", // Module ID
    /* List of module artifacts */
    "artifacts" : [ {
      "type" : "jar",
      "sha1" : "2ed52ad1d864aec00d7a2394e99b3abca6d16639",
      "md5" : "844920070d81271b69579e17ddc6715c",
      "name" : "multi2-4.2-SNAPSHOT.jar"
    }, {
      "type" : "pom",
      "sha1" : "e8e9c7d790191f4a3df3a82314ac590f45c86e31",
      "md5" : "1f027d857ff522286a82107be9e807cd",
      "name" : "multi2-4.2-SNAPSHOT.pom"
    } ],
    /* List of module dependencies */
    "dependencies" : [ {
      "type" : "jar",
      "sha1" : "99129f16442844f6a4a11ae22fbbee40b14d774f",
      "md5" : "1f40fb782a4f2cf78f161d32670f7a3a",
      "id" : "junit:junit:3.8.1",
      "scopes" : [ "test" ]
    } ]
  }, { // The build's second module
    "properties" : { // Module properties
      "project.build.sourceEncoding" : "UTF-8"
    },
    "id" : "org.jfrog.test:multi3:4.2-SNAPSHOT", // Module ID
    /* List of module artifacts */	
    "artifacts" : [ { // Module artifacts
      "type" : "war",
      "sha1" : "df8e7d7b94d5ec9db3bfc92e945c7ff4e2391c7c",
      "md5" : "423c24f4c6e259f2774921b9d874a649",
      "name" : "multi3-4.2-SNAPSHOT.war"
    }, {
      "type" : "pom",
      "sha1" : "656330c5045130f214f954643fdc4b677f4cf7aa",
      "md5" : "b0afa67a9089b6f71b3c39043b18b2fc",
      "name" : "multi3-4.2-SNAPSHOT.pom"
    } ],
    /* List of module dependencies */
    "dependencies" : [ {
      "type" : "jar",
      "sha1" : "a8762d07e76cfde2395257a5da47ba7c1dbd3dce",
      "md5" : "b6a50c8a15ece8753e37cbe5700bf84f",
      "id" : "commons-io:commons-io:1.4",
      "scopes" : [ "compile" ]
    }, {
      "type" : "jar",
      "sha1" : "342d1eb41a2bc7b52fa2e54e9872463fc86e2650",
      "md5" : "2a666534a425add50d017d4aa06a6fca",
      "id" : "org.codehaus.plexus:plexus-utils:1.5.1",
      "scopes" : [ "compile" ]
    }, {
      "type" : "jar",
      "sha1" : "449ea46b27426eb846611a90b2fb8b4dcf271191",
      "md5" : "25c0752852205167af8f31a1eb019975",
      "id" : "org.springframework:spring-beans:2.5.6",
      "scopes" : [ "compile" ]
    } ]
  } ],
  /* List of issues related to the build */
    "issues" : {
    "tracker" : {
      "name" : "JIRA",
      "version" : "6.0.1"
    },
    "aggregateBuildIssues" : true,  //whether or not there are issues that already appeared in previous builds
    "aggregationBuildStatus" : "Released",
    "affectedIssues" : [ {
      "key" : "RTFACT-1234",
      "url" : "https://www.jfrog.com/jira/browse/RTFACT-1234",
      "summary" : "Description of the relevant issue",
      "aggregated" : false  //whether or not this specific issue already appeared in previous builds
    }, {
      "key" : "RTFACT-5469",
      "url" : "https://www.jfrog.com/jira/browse/RTFACT-5678",
      "summary" : "Description of the relevant issue",
      "aggregated" : true
    } ] 
  },  
  "governance" : { // Black Duck Code Center integration information
    "blackDuckProperties" : {
      "appName" : "", // The Black Duck Code Center application name
      "appVersion" : "", // The Black Duck Code Center application version
      "reportRecipients" : "", // Recipients that should receive an email report once the automatic Black Duck Code Center compliance checks are completed (space-separated list)
      "scopes" : "", // A list of dependency scopes/configurations to run Black Duck Code Center compliance checks on. If left empty all dependencies from all scopes will be checked  (space-separated list)
      "runChecks" : true, // Should Black Duck Code Center run automatic compliance checks after the build is completed (true/false)
      "includePublishedArtifacts" : true, // Include the build's published module artifacts in the Black Duck Code Center compliance checks if they are also used as dependencies for other modules in this build  (true/false)
      "autoCreateMissingComponentRequests" : true, // Auto create missing components in Black Duck Code Center application after the build is completed and deployed in Artifactory (true/false)
      "autoDiscardStaleComponentRequests" : true // Auto discard stale components in Black Duck Code Center application after the build is completed and deployed in Artifactory (true/false)
    }
  }
}
```

### Build Info json schema
```groovy
{
  "$schema": "http://json-schema.org/draft-04/schema#",
  "title": "build-info",
  "description": "Artifactory build-info",
  "type": "object",
  "properties": {
    "version": {
      "description": "Build info schema version",
      "type": "string"
    },
    "name": {
      "description": "Build name",
      "type": "string"
    },
    "number": {
      "description": "Build number",
      "type": "string"
    },
    "type": {
      "description": "Build type",
      "type": "string",
      "enum": [ "MAVEN", "GRADLE", "ANT", "IVY", "GENERIC" ]
    },
    "buildAgent": {
      "description": "Build tool information",
      "type": "object",
      "properties": {
        "name": {
          "description": "Build tool type",
          "type": "string"
        },
        "version": {
          "description": "Build tool version",
          "type": "string"
        }
      },
      "required": [ "name", "version" ]
    },
    "agent": {
      "description": "CI server information",
      "type": "object",
      "properties": {
        "name": {
          "description": "CI server type",
          "type": "string"
        },
        "version": {
          "description": "CI server version",
          "type": "string"
        }
      },
      "required": [ "name", "version" ]
    },
    "started": {
      "description": "Build start time",
      "type": "string",
      "pattern": "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.\\d{3}(Z|[+-]\\d{4})$"
    },
    "durationMillis": {
      "description": "Build duration in milliseconds",
      "type": "integer"
    },
    "artifactoryPrincipal": {
      "description": "Artifactory user used for deployment",
      "type": "string"
    },
    "url": {
      "description": "CI server URL",
      "type": "string"
    },
    "vcsRevision": {
      "description": "VCS revision",
      "type": "string"
    },
    "vcsUrl": {
      "description": "VCS URL",
      "type": "string"
    },
    "licenseControl": {
      "description": "Artifactory License Control Information",
      "type": "object",
      "properties": {
        "runChecks": {
          "description": "Run automatic license scanning after the build is complete",
          "type": "boolean"
        },
        "includePublishedArtifacts": {
          "description": "Run license checks on artifacts in addition to build dependencies",
          "type": "boolean"
        },
        "autoDiscover": {
          "description": "Artifactory should auto-discover license",
          "type": "boolean"
        },
        "scopesList": {
          "description": "Space-separated list of dependency scopes to run license violation checks",
          "type": "string"
        },
        "licenseViolationsRecipientsList": {
          "description": "Space-separated list of email addresses to be notified of license violations",
          "type": "string"
        }
      },
      "required": [ "runChecks", "includePublishedArtifacts", "autoDiscover", "scopesList", "licenseViolationsRecipientsList" ]
    },
    "buildRetention": {
      "description": "Build Retention Information",
      "type": "object",
      "allOf": [
        {
          "properties": {
            "deleteBuildArtifacts": {
              "description": "Automatically remove build artifacts stored in Artifactory",
              "type": "boolean"
            },
            "buildNumbersNotToBeDiscarded": {
              "description": "List of build numbers that should not be removed from Artifactory",
              "type": "array",
              "items": {
                "type": "integer"
              }
            }
          }
        },
        {
          "anyOf": [
            {
              "properties": {
                "count": {
                  "description": "Maximum number of builds to store in Artifactory",
                  "type": "integer"
                }
              },
              "required": [ "count" ]
            },
            {
              "properties": {
                "minimumBuildDate": {
                  "description": "Earliest build date to store in Artifactory",
                  "type": "integer"
                }
              },
              "required": [ "minimumBuildDate" ]
            }
          ]
        }
      ]
    },
    "modules": {
      "description": "Artifactory License Control Information",
      "type": "array",
      "items": {
        "type": "object",
        "properties": {
          "properties": {
             "description": "Module properties",
             "type": "object",
             "patternProperties": {
                "^.+$": {
                  "type": "string"
                }
             }
          },
          "id": {
            "description": "Module ID",
            "type": "string"
          },
          "artifacts": {
            "description": "List of module artifacts",
            "type": "array",
            "items": {
              "type": "object",
              "allOf": [
                {
                  "properties": {
                    "type": {
                      "type": "string"
                    },
                    "name": {
                      "type": "string"
                    }
                  },
                  "required": [ "type", "name" ]
                },
                {
                  "$ref": "#/definitions/hash"
                }
              ]
            }
          },
          "dependencies": {
            "description": "List of module dependencies",
            "type": "array",
            "items": {
              "type": "object",
              "allOf": [
                {
                  "properties": {
                    "type": {
                      "type": "string"
                    },
                    "id": {
                      "type": "string"
                    },
                    "scopes": {
                      "type": "array",
                      "items": {
                        "type": "string"
                      }
                    }
                  },
                  "required": [ "type", "id" ]
                },
                {
                  "$ref": "#/definitions/hash"
                }
              ]
            }
          }
        },
        "required": [ "id", "artifacts", "dependencies" ]
      }
    },
    "issues": {
      "description": "List of issues related to the build",
      "type": "object",
      "properties": {
        "tracker": {
          "type": "object",
          "properties": {
            "name": {
              "type": "string"
            },
            "version": {
              "type": "string"
            }
          },
          "required": [ "name", "version" ]
        },
        "aggregateBuildIssues": {
          "description": "Whether issues have appeared in previous builds",
          "type": "boolean"
        },
        "aggregationBuildStatus": {
          "type": "string"
        },
        "affectedIssues": {
          "type": "array",
          "items": {
            "type": "object",
            "properties": {
              "key": {
                "type": "string"
              },
              "url": {
                "type": "string"
              },
              "summary": {
                "type": "string"
              },
              "aggregated": {
                "description": "Whether this specific issue already appeared in previous builds",
                "type": "boolean"
              }
            },
            "required": [ "key", "url", "summary", "aggregated" ]
          }
        }
      },
      "required": [ "tracker", "aggregateBuildIssues", "aggregationBuildStatus", "affectedIssues" ]
    },
    "governence": {
      "description": "Black duck code center integration information",
      "type": "object",
      "properties": {
        "blackDuckProperties": {
          "type": "object",
          "properties": {
            "appName": {
              "description": "The Black Duck Code Center application name",
              "type": "string"
            },
            "appVersion": {
              "description": "The Black Duck Code Center application version",
              "type": "string"
            },
            "reportRecipients": {
              "description": "Space-separated list of recipients that should receive an email report once the automatic Black Duck Code Center compliance checks are completed",
              "type": "string"
            },
            "scopes": {
              "description": "Space-separated list of dependency scopes/configurations to run Black Duck Code Center compliance checks on. If left empty all dependencies from all scopes will be checked",
              "type": "string"
            },
            "runChecks": {
              "description": "Should Black Duck Code Center run automatic compliance checks after the build is completed",
              "type": "boolean"
            },
            "includePublishedArtifacts": {
              "description": "Include the build's published module artifacts in the Black Duck Code Center compliance checks if they are also used as dependencies for other modules in this build",
              "type": "boolean"
            },
            "autoCreateMissingComponentRequests": {
              "description": "Auto create missing components in Black Duck Code Center application after the build is completed and deployed in Artifactory",
              "type": "boolean"
            },
            "autoDiscardStaleComponentRequests": {
              "description": "Auto discard stale components in Black Duck Code Center application after the build is completed and deployed in Artifactory",
              "type": "boolean"
            }
          },
          "required": [ "appName", "appVersion", "reportRecipients", "scopes", "runChecks", "includePublishedArtifacts", "autoCreateMissingComponentRequests", "autoDiscardStaleComponentRequests" ]
        }
      }
    }
  },
  "required": [ "version", "name", "number", "type", "started", "durationMillis", "modules" ],
  "definitions": {
    "sha1": {
      "description": "sha1 hash",
      "type": "string",
      "pattern": "^[0-9a-f]{40}$"
    },
    "md5": {
      "description": "md5 hash",
      "type": "string",
      "pattern": "^[0-9a-f]{32}$"
    },
    "hash": {
      "description": "md5, sha1, or both",
      "anyOf": [
        {
          "properties": {
            "sha1": {
              "$ref": "#/definitions/sha1"
            }
          },
          "required": [ "sha1" ]
        },
        {
          "properties": {
            "md5": {
              "$ref": "#/definitions/md5"
            }
          },
          "required": [ "md5" ]
        }
      ]
    }
  }
}
```
