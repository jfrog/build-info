package org.jfrog.gradle.plugin.artifactory.dsl

import org.gradle.api.Project
import org.gradle.util.ConfigureUtil

class DistributerConfig {
    private final Project project

    private String contextUrl
    private String gpgPassphrase
    private String targetRepoKey
    private String username
    private String password
    private HashSet<String> sourceRepoKeys
    private boolean publish
    private boolean overrideExistingFiles
    private boolean async
    private boolean dryRun

    DistributerConfig(ArtifactoryPluginConvention conv) {
        project = conv.project
    }

    def config(Closure closure) {
        ConfigureUtil.configure(closure, this)
    }

    boolean getPublish() {
        return publish
    }

    void setPublish(boolean publish) {
        this.publish = publish
    }

    String getContextUrl() {
        return contextUrl
    }

    void setContextUrl(String contextUrl) {
        this.contextUrl = contextUrl
    }

    String getGpgPassphrase() {
        return gpgPassphrase
    }

    void setGpgPassphrase(String gpgPassphrase) {
        this.gpgPassphrase = gpgPassphrase
    }

    String getTargetRepoKey() {
        return targetRepoKey
    }

    void setTargetRepoKey(String targetRepoKey) {
        this.targetRepoKey = targetRepoKey
    }

    HashSet<String> getSourceRepoKeys() {
        return sourceRepoKeys
    }

    void setSourceRepoKeys(HashSet<String> sourceRepoKeys) {
        this.sourceRepoKeys = sourceRepoKeys
    }

    boolean getOverrideExistingFiles() {
        return overrideExistingFiles
    }

    void setOverrideExistingFiles(boolean overrideExistingFiles) {
        this.overrideExistingFiles = overrideExistingFiles
    }

    boolean getAsync() {
        return async
    }

    void setAsync(boolean async) {
        this.async = async
    }

    boolean getDryRun() {
        return dryRun
    }

    void setDryRun(boolean dryRun) {
        this.dryRun = dryRun
    }

    String getUsername() {
        return username
    }

    void setUsername(String username) {
        this.username = username
    }

    String getPassword() {
        return password
    }

    void setPassword(String password) {
        this.password = password
    }
}
