plugins {
    id("com.jfrog.artifactory") apply false
}

fun javaProjects() = subprojects.filter {
    File(it.projectDir, "src").isDirectory
}

val currentVersion: String by project

allprojects {
    apply(plugin = "com.jfrog.artifactory")

    group = "org.jfrog.test.gradle.publish"
    version = currentVersion
    status = "Integration"

    repositories {
        maven(url = "${System.getenv("BITESTS_PLATFORM_URL")}/artifactory/${System.getenv("BITESTS_ARTIFACTORY_VIRTUAL_REPO")}") {
            credentials {
                username = System.getenv("BITESTS_PLATFORM_USERNAME")
                password = System.getenv("BITESTS_PLATFORM_PASSWORD")
            }
        }
    }
}

tasks {
    named<org.jfrog.gradle.plugin.artifactory.task.ArtifactoryTask>("artifactoryPublish") {
        skip = true
    }
}

project("services") {
    tasks {
        named<org.jfrog.gradle.plugin.artifactory.task.ArtifactoryTask>("artifactoryPublish") {
            skip = true
        }
    }
}

configure(javaProjects()) {
    apply {
        plugin("java")
        plugin("maven-publish")
        plugin("ivy-publish")
    }

    configure<PublishingExtension> {
        publications {
            register<MavenPublication>("mavenJava") {
                from(components.getByName("java"))
                artifact(file("$rootDir/gradle.properties"))
            }
            register<IvyPublication>("ivyJava") {
                from(components.getByName("java"))
            }
        }
    }
}

configure<org.jfrog.gradle.plugin.artifactory.dsl.ArtifactoryPluginConvention> {
    clientConfig.isIncludeEnvVars = true
    clientConfig.info.addEnvironmentProperty("test.adding.dynVar", java.util.Date().toString())

    setContextUrl(System.getenv("BITESTS_PLATFORM_URL")+"/artifactory")
    publish {
        repository {
            setRepoKey(System.getenv("BITESTS_ARTIFACTORY_LOCAL_REPO")) // The Artifactory repository key to publish to
            setUsername(System.getenv("BITESTS_PLATFORM_USERNAME")) // The publisher user name
            setPassword(System.getenv("BITESTS_PLATFORM_PASSWORD")) // The publisher password
            // This is an optional section for configuring Ivy publication (when publishIvy = true).
            ivy {
                setIvyLayout("[organization]/[module]/ivy-[revision].xml")
                setArtifactLayout("[organization]/[module]/[revision]/[module]-[revision](-[classifier]).[ext]")
                setMavenCompatible(true)
            }
        }

        defaults {
            publications("mavenJava", "ivyJava")
            setPublishArtifacts(true)
            // Properties to be attached to the published artifacts.
            setPublishPom(true) // Publish generated POM files to Artifactory (true by default)
            setPublishIvy(true) // Publish generated Ivy descriptor files to Artifactory (true by default)
        }
    }
}

