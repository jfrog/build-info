# Release Notes

## build-info-extractor 2.26.0 / gradle-artifactory-plugin 4.24.0 (May 19, 2021)
- Upgrade xstream to 1.4.16 ([489](https://github.com/jfrog/build-info/pull/489)) 
- Add support for npm 7.7([484](https://github.com/jfrog/build-info/pull/484))
- Add support JFrog platform URL for published build ([478](https://github.com/jfrog/build-info/pull/478))
- Add support for Artifactory Projects ([471](https://github.com/jfrog/build-info/pull/471))
- Add support for NuGet V3  protocol ([479](https://github.com/jfrog/build-info/pull/479) & [494](https://github.com/jfrog/build-info/pull/494))
- Bug fix - IDE scan fix top available issue severity should be critical ([488](https://github.com/jfrog/build-info/pull/488))
- Bug fix - env not collect in npm, Go, Pip, and NuGet builds ([491](https://github.com/jfrog/build-info/pull/491))

## build-info-extractor 2.25.4 / gradle-artifactory-plugin 4.23.4 (April 29, 2021)
- Start uploading build-info JARs to Maven Central
- Bug fix - Gradle circular dependency resolution causes stack overflow
- Bug fix - NPE is thrown if the image is not found in Artifactory

## build-info-extractor 2.25.0 / gradle-artifactory-plugin 4.23.0 (March 31, 2021)
- Gradle Kotlin DSL support 
- Update severities in dependency tree - make 'Critical' one level above 'High'
- Bug fix - Usage report failure when Artifactory return 202

## build-info-extractor 2.24.1 / gradle-artifactory-plugin 4.22.1 (March 22, 2021)
- Populate requestedBy field in Gradle and NPM build-info
- Update Artifactory image URL to releases-docker.jfrog.io in README
- Bug fix - upload files with props omit multiple slashes
- Bug fix - Git-collect-issues should ignore errors when the revision of the previous build info doesn't exist.
