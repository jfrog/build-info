# Release Notes

| Release notes moved to https://github.com/jfrog/build-info/releases |
|----------------------------------------------------------------------------------------------------------------------------------------------------------|

## build-info-extractor 2.39.9 / gradle-artifactory-plugin 4.31.9 (April 9, 2023)
- Add Client ID to the usage report ([717](https://github.com/jfrog/build-info/pull/717))

## build-info-extractor 2.39.8 / gradle-artifactory-plugin 4.31.8 (March 23, 2023)
- Refactor 'collectAndFilterEnvIfNeeded' func ([721](https://github.com/jfrog/build-info/pull/721))

## build-info-extractor 2.39.7 / gradle-artifactory-plugin 4.31.7 (March 15, 2023)
- Fix missing Maven 3.9.0 requirement field ([718](https://github.com/jfrog/build-info/pull/718))

## build-info-extractor 2.39.6 / gradle-artifactory-plugin 4.31.6 (March 13, 2023)
- Filter build-info and modules properties ([716](https://github.com/jfrog/build-info/pull/716))

## build-info-extractor 2.39.5 / gradle-artifactory-plugin 4.31.5 (March 2, 2023)
- Revert "Removed the dummy remote repository in favour for the snapshot and re…" ([714](https://github.com/jfrog/build-info/pull/714))

## build-info-extractor 2.39.4 / gradle-artifactory-plugin 4.31.4 (February 20, 2023)
- Adding Go Get command to go driver ([709](https://github.com/jfrog/build-info/pull/709))

## build-info-extractor 2.39.3 / gradle-artifactory-plugin 4.31.3 (February 16, 2023)
- Refactor and handle unhandled exceptions ([707](https://github.com/jfrog/build-info/pull/707))

## build-info-extractor 2.39.2 / gradle-artifactory-plugin 4.31.2 (February 15, 2023)
- Fix missing fields for maven 3.9.0 ([706](https://github.com/jfrog/build-info/pull/706))

## build-info-extractor 2.39.1 / gradle-artifactory-plugin 4.31.1 (January 7, 2023)
- Retry on error codes 500 ([694](https://github.com/jfrog/build-info/pull/694))

## build-info-extractor 2.39.0 / gradle-artifactory-plugin 4.31.0 (December 29, 2022)
- Allow skipping VCS stamping in Go ([689](https://github.com/jfrog/build-info/pull/689))
- Remove the dummy remote repository in Maven ([683](https://github.com/jfrog/build-info/pull/683))

## build-info-extractor 2.38.1 / gradle-artifactory-plugin 4.30.1 (December 15, 2022)
- Downgrade slf4j to 1.7.36 ([686](https://github.com/jfrog/build-info/pull/686))

## build-info-extractor 2.38.0 / gradle-artifactory-plugin 4.30.0 (December 14, 2022)
- Add getFederatedRepositoriesKeys to ArtifactoryManager ([685](https://github.com/jfrog/build-info/pull/685)) 
- Gradle - Publish bom files when 'java-platform' plugin applied ([680](https://github.com/jfrog/build-info/pull/680))
- Update dependencies and support building Java 11 ([659](https://github.com/jfrog/build-info/pull/659))
- Bug fix - NullPointerException when building the matrix params ([684](https://github.com/jfrog/build-info/pull/684))

## build-info-extractor 2.37.3 / gradle-artifactory-plugin 4.29.3 (November 10, 2022)
- Bug fix - Remove custom exception for unknown violation types ([675](https://github.com/jfrog/build-info/pull/675))
- Bug fix - Sha256 hashes being set to literal "SHA-256" in build-info json ([669](https://github.com/jfrog/build-info/pull/669))

## build-info-extractor 2.37.2 / gradle-artifactory-plugin 4.29.2 (October 18, 2022)
- Bug fix - Pom doesn't get deployed using Maven Install plugin 3+ ([670](https://github.com/jfrog/build-info/pull/670))

## build-info-extractor 2.37.1 / gradle-artifactory-plugin 4.29.1 (September 28, 2022)
- Bug fix - Support Docker module ID with slash ([666](https://github.com/jfrog/build-info/pull/666))

## build-info-extractor 2.37.0 / gradle-artifactory-plugin 4.29.0 (July 21, 2022)
- Add project field to gradle extractor ([661](https://github.com/jfrog/build-info/pull/661))

## build-info-extractor 2.36.3 / gradle-artifactory-plugin 4.28.3 (May 12, 2022)
- Bug fix - Missing Gradle build-info props on artifacts ([649](https://github.com/jfrog/build-info/pull/649))

## build-info-extractor 2.36.4 / gradle-artifactory-plugin 4.28.4 (July 3, 2022)
- Upgrade httpcore to 4.4.13 ([654](https://github.com/jfrog/build-info/pull/654))
- Bug fix - Broken AQL request when build is not defined in FilesGroup ([650](https://github.com/jfrog/build-info/pull/650))

## build-info-extractor 2.36.3 / gradle-artifactory-plugin 4.28.3 (May 12, 2022)
- Bug fix - Missing Gradle build-info props on artifacts ([649](https://github.com/jfrog/build-info/pull/649))

## build-info-extractor 2.36.2 / gradle-artifactory-plugin 4.28.2 (April 20, 2022)
- Bug fix - Incorrect plugins endpoints ([635](https://github.com/jfrog/build-info/pull/635))
- Bug fix - When project provided, "projectKey" query parameter should be added to build info URL ([631](https://github.com/jfrog/build-info/pull/631))
- Bug fix - Go driver should allow ignoring errors "go mod tidy" as it does in "go mod graph" ([634](https://github.com/jfrog/build-info/pull/634))
- Bug fix - Cve should have an empty constructor to allow serialization ([644](https://github.com/jfrog/build-info/pull/644))
- Bug fix - Incorrect Docker manifest path when collecting build-info ([643](https://github.com/jfrog/build-info/pull/643))

## build-info-extractor 2.36.1 / gradle-artifactory-plugin 4.28.1 (March 27, 2022)
- Remove unnecessary warning log message about missing branch ([632](https://github.com/jfrog/build-info/pull/632))
- Remove the usage of StringUtils.firstNonBlank ([633](https://github.com/jfrog/build-info/pull/633))

## build-info-extractor 2.36.0 / gradle-artifactory-plugin 4.28.0 (March 16, 2022)
- Go - Filter out unused dependencies from build info ([622](https://github.com/jfrog/build-info/pull/622))
- Gradle - Support proxy in build info publish ([629](https://github.com/jfrog/build-info/pull/629))
- Maven - Deploy only in mvn install/deploy phases ([626](https://github.com/jfrog/build-info/pull/626)) & ([630](https://github.com/jfrog/build-info/pull/630))
- Bug fix - Maven artifacts properties not set ([624](https://github.com/jfrog/build-info/pull/624))
- Bug fix - in some cases, the build info URL is wrong ([618](https://github.com/jfrog/build-info/pull/618)) & ([619](https://github.com/jfrog/build-info/pull/619))
- Bug fix - In some cases, the project parameter in Maven is ignored ([618](https://github.com/jfrog/build-info/pull/618))
- Bug fix - in some cases, Docker module name in build-info is wrong ([617](https://github.com/jfrog/build-info/pull/617))

## build-info-extractor 2.35.0 / gradle-artifactory-plugin 4.27.1 (February 13, 2022)
- Add project support for Maven ([612](https://github.com/jfrog/build-info/pull/612))
- IDEs - Support ignore URL and references to issues in the dependency tree ([611](https://github.com/jfrog/build-info/pull/611))
- Bug fix - Maven deploy fails when the classifier string value is too long ([612](https://github.com/jfrog/build-info/pull/612))

## build-info-extractor 2.34.0 / gradle-artifactory-plugin 4.27.0 (February 6, 2022)
- Support build promotion with projects ([613](https://github.com/jfrog/build-info/pull/613))
- Support insecure TLS in the Gradle Artifactory plugin ([610](https://github.com/jfrog/build-info/pull/610))

## build-info-extractor 2.33.3 / gradle-artifactory-plugin 4.26.3 (January 26, 2022)
- Bug fix - Signature of AmazonCorrettoCryptoProvider couldn't found ([609](https://github.com/jfrog/build-info/pull/609))

## build-info-extractor 2.33.2 / gradle-artifactory-plugin 4.26.2 (January 20, 2022)
- Bug fix - Add 'localpath' to artifact builder ([606](https://github.com/jfrog/build-info/pull/606))
- Add SHA2 to upload files ([605](https://github.com/jfrog/build-info/pull/605))

## build-info-extractor 2.33.1 / gradle-artifactory-plugin 4.26.1 (January 6, 2022)
- Remove JGIT dependency ([600](https://github.com/jfrog/build-info/pull/600))

## build-info-extractor 2.33.0 / gradle-artifactory-plugin 4.26.0 (January 5, 2022)
- Calculate SHA256 in builds ([598](https://github.com/jfrog/build-info/pull/598))

## build-info-extractor 2.32.6 / gradle-artifactory-plugin 4.25.5 (January 3, 2022)
- Add VCS branch and VCS message to build-info VCS details ([591](https://github.com/jfrog/build-info/pull/591))
- Bug fix - VCS properties on artifacts are not added on maven late deploy ([596](https://github.com/jfrog/build-info/pull/596))

## build-info-extractor 2.32.5 / gradle-artifactory-plugin 4.25.4 (December 28, 2021)
- Add support for v2 compatible Go projects with submodule as root ([594](https://github.com/jfrog/build-info/pull/594))
- Bugfix - Gradle defaults props should not ignore deprecated build-info properties ([590](https://github.com/jfrog/build-info/pull/590))
- IDEs - Compare scan issues by ID ([589](https://github.com/jfrog/build-info/pull/589))

## build-info-extractor 2.32.4 / gradle-artifactory-plugin 4.25.3 (December 19, 2021)
- Deprecate 'artifactory.' property prefix.
- Bug fix - Multi Values Properties Set ([583](https://github.com/jfrog/build-info/pull/583))

## build-info-extractor 2.32.3 / gradle-artifactory-plugin 4.25.2 (December 7, 2021)
- Update file-specs-java dependency to 1.1.1

## build-info-extractor 2.32.2 / gradle-artifactory-plugin 4.25.1 (December 5, 2021)
- Update apache commons-lang (2) to current commons-lang3 ([580](https://github.com/jfrog/build-info/pull/580))
- Allow ignoring go list errors ([577](https://github.com/jfrog/build-info/pull/577))
- Bugfix - Build-info ignores duplicate artifacts checksum ([579](https://github.com/jfrog/build-info/pull/579))

## build-info-extractor 2.32.0 / gradle-artifactory-plugin 4.25.0 (November 30, 2021)
- Add 'mvn deploy' command.
 
## build-info-extractor 2.31.2 / gradle-artifactory-plugin 4.24.21 (November 3, 2021)
- Add CVE ID to Xray scan Issue ([564](https://github.com/jfrog/build-info/pull/564))
- Improve dependency tree performance and memory consumption ([562](https://github.com/jfrog/build-info/pull/562)) ([565](https://github.com/jfrog/build-info/pull/565))

## build-info-extractor 2.31.1 / gradle-artifactory-plugin 4.24.20 (October 1, 2021)
- Add metadata field to dependency tree node ([558](https://github.com/jfrog/build-info/pull/558))

## build-info-extractor 2.31.0 / gradle-artifactory-plugin 4.24.19 (September 30, 2021)
- Use the file-specs-java library ([552](https://github.com/jfrog/build-info/pull/552))
- Add getUsedModules and modTidy to GoDriver ([557](https://github.com/jfrog/build-info/pull/557))
- Add support for new Xray graph scan ([556](https://github.com/jfrog/build-info/pull/556))

## build-info-extractor 2.30.2 / gradle-artifactory-plugin 4.24.18 (September 19, 2021)
- Bug fix - IllegalArgumentException during build scan ([554](https://github.com/jfrog/build-info/pull/554))
- Dependencies update ([553](https://github.com/jfrog/build-info/pull/553))

## build-info-extractor 2.30.0 / gradle-artifactory-plugin 4.24.16 (August 17, 2021)
- Add Projects APIs ([549](https://github.com/jfrog/build-info/pull/549))
- Run Go on new Java process ([548](https://github.com/jfrog/build-info/pull/548))

## build-info-extractor 2.29.0 / gradle-artifactory-plugin 4.24.15 (August 15, 2021)
- Refactor build scan table ([545](https://github.com/jfrog/build-info/pull/545))
- Allow npm projects without name and versions ([546](https://github.com/jfrog/build-info/pull/546))

## build-info-extractor 2.28.8 / gradle-artifactory-plugin 4.24.14 (July 19, 2021)
- Bug fix - Fail to deserialize deployable artifacts ([537](https://github.com/jfrog/build-info/pull/537))
- Gradle - skip uploading JAR if no jar produced in build ([538](https://github.com/jfrog/build-info/pull/538))
- Make download headers comparisons case insensitive ([539](https://github.com/jfrog/build-info/pull/539))

## build-info-extractor 2.28.6 / gradle-artifactory-plugin 4.24.12 (July 13, 2021)
- Separate target repository and artifacts destination in the deployable artifacts file ([532](https://github.com/jfrog/build-info/pull/532))
- Publish Gradle plugin to Gradle Gallery ([536](https://github.com/jfrog/build-info/pull/536))

## build-info-extractor 2.28.5 / gradle-artifactory-plugin 4.24.11 (July 8, 2021)
- Add Artifactory url and repository to artifacts destination in deployable artifacts file ([530](https://github.com/jfrog/build-info/pull/530))

## build-info-extractor 2.28.4 / gradle-artifactory-plugin 4.24.10 (July 5, 2021)
- Bug fix - ReportUsage throws an exception for Artifactory version 6.9.0 ([525](https://github.com/jfrog/build-info/pull/525))

## build-info-extractor 2.28.3 / gradle-artifactory-plugin 4.24.9 (June 30, 2021)
- Bug fix - file-spec-java missing dependency issue when using the Artifactory Gradle plugin ([523](https://github.com/jfrog/build-info/pull/523))

## build-info-extractor 2.28.2 / gradle-artifactory-plugin 4.24.8 (June 29, 2021)
- Add support for Kaniko and JIB ([512](https://github.com/jfrog/build-info/pull/512))
- Add JFrog Distribution commands ([520](https://github.com/jfrog/build-info/pull/520))
- Add project to ScanBuild service ([519](https://github.com/jfrog/build-info/pull/519))
- Bug fix - NoSuchMethodError when using IOUtils.toString ([516](https://github.com/jfrog/build-info/pull/516))

## build-info-extractor 2.27.0 / gradle-artifactory-plugin 4.24.5 (June 16, 2021)
- Add sha256 to the upload response of maven and gradle ([477](https://github.com/jfrog/build-info/pull/477))
- Update xstream to 1.4.17 ([513](https://github.com/jfrog/build-info/pull/513))

## build-info-extractor 2.26.4 / gradle-artifactory-plugin 4.24.4 (May 31, 2021)
- Bug fix - Error when trying to download an empty (zero bytes size) file ([507](https://github.com/jfrog/build-info/pull/507))
- Bug fix - Deploy file doesn't print full URL in the log output ([509](https://github.com/jfrog/build-info/pull/509))

## build-info-extractor 2.26.3 / gradle-artifactory-plugin 4.24.3 (May 28, 2021)
- Bug fix - Ignore missing fields while deserialize HTTP response ([502](https://github.com/jfrog/build-info/pull/502))
- Bug fix - Build retention service sends an empty body ([504](https://github.com/jfrog/build-info/pull/504))
- Bug fix - Artifactory Trigger cannot deserialize instance ItemLastModified ([503](https://github.com/jfrog/build-info/pull/503))

## build-info-extractor 2.26.2 / gradle-artifactory-plugin 4.24.2 (May 25, 2021)
- Gradle - Improve Gradle transitive dependency collection ([498](https://github.com/jfrog/build-info/pull/498))
- Restructuring Artifactory APIs ([493](https://github.com/jfrog/build-info/pull/493))

## build-info-extractor 2.26.1 / gradle-artifactory-plugin 4.24.1 (May 23, 2021)
- Gradle - Build-info is not published, if the build does not deploy artifacts ([495](https://github.com/jfrog/build-info/pull/495))

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
