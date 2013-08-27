package org.jfrog.gradle.plugin.artifactory.dsl

import org.gradle.api.Project

/**
 * @author freds
 */
class DoubleDelegateWrapper {
    private final def realDelegate
    private final Project project

    DoubleDelegateWrapper(Project project, def realDelegate) {
        this.project = project
        this.realDelegate = realDelegate
    }

    @Override
    Object invokeMethod(String name, Object args) {
        return realDelegate.invokeMethod(name, args)
    }

    def propertyMissing(String name) {
        if (realDelegate.hasProperty(name)) realDelegate."$name"
        else project.property(name)
    }

    def propertyMissing(String name, def value) {
        realDelegate."$name" = value
    }
}
