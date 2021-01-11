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
        maven(url = "${System.getenv("BITESTS_ARTIFACTORY_URL")}/${System.getenv("BITESTS_ARTIFACTORY_VIRTUAL_REPO")}") {
            credentials {
                username = System.getenv("BITESTS_ARTIFACTORY_USERNAME")
                password = System.getenv("BITESTS_ARTIFACTORY_PASSWORD")
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

    setContextUrl(System.getenv("BITESTS_ARTIFACTORY_URL"))
    publish(delegateClosureOf<org.jfrog.gradle.plugin.artifactory.dsl.PublisherConfig> {
        repository(delegateClosureOf<groovy.lang.GroovyObject> {
            setProperty("repoKey", System.getenv("BITESTS_ARTIFACTORY_LOCAL_REPO")) // The Artifactory repository key to publish to
            setProperty("username", System.getenv("BITESTS_ARTIFACTORY_USERNAME")) // The publisher user name
            setProperty("password", System.getenv("BITESTS_ARTIFACTORY_PASSWORD")) // The publisher password
            // This is an optional section for configuring Ivy publication (when publishIvy = true).
            setProperty("ivy", delegateClosureOf<groovy.lang.GroovyObject> {
                setProperty("ivyLayout", "[organization]/[module]/ivy-[revision].xml")
                setProperty("artifactLayout", "[organization]/[module]/[revision]/[module]-[revision](-[classifier]).[ext]")
                setProperty("mavenCompatible", true)
            })
        })

        defaults(delegateClosureOf<org.jfrog.gradle.plugin.artifactory.task.ArtifactoryTask> {
            publications("mavenJava", "ivyJava")
            setPublishArtifacts(true)
            // Properties to be attached to the published artifacts.
            setPublishPom(true) // Publish generated POM files to Artifactory (true by default)
            setPublishIvy(true) // Publish generated Ivy descriptor files to Artifactory (true by default)
        })
    })
}

