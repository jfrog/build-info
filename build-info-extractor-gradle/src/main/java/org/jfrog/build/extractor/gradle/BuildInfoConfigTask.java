package org.jfrog.build.extractor.gradle;

import groovy.lang.Closure;
import org.apache.commons.lang.StringUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.tasks.TaskAction;
import org.gradle.util.ConfigureUtil;
import org.jfrog.build.ArtifactoryPluginUtils;
import org.jfrog.build.api.BuildInfoFields;
import org.jfrog.build.client.ArtifactoryClientConfiguration;
import org.jfrog.build.extractor.logger.GradleClientLogger;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * @author freds
 *         Date: 1/10/11
 *         Time: 3:59 PM
 */
public class BuildInfoConfigTask extends DefaultTask {

    protected ArtifactoryClientConfiguration acc;

    private List<Closure> resolverConfigure;
    private List<Closure> publisherConfigure;
    private List<Closure> infoConfigure;
    private List<Closure> proxyConfigure;

    public BuildInfoConfigTask() {
        // Create the empty dummy acc that will received configured properties from gradle code
        // TODO: Should not be populated right now
        acc = ArtifactoryPluginUtils.getArtifactoryClientConfiguration(getProject());
    }

    @TaskAction
    void fillClientConfiguration() {
        ArtifactoryClientConfiguration finalAcc = ArtifactoryPluginUtils.getArtifactoryClientConfiguration(getProject());
        // TODO: Should copy only root fields
        finalAcc.fillFromProperties(acc.getAllProperties());
        acc = finalAcc;
        for (Closure closure : resolverConfigure) {
            ConfigureUtil.configure(closure, acc.resolver);
        }
        for (Closure closure : publisherConfigure) {
            ConfigureUtil.configure(closure, acc.publisher);
        }
        for (Closure closure : infoConfigure) {
            ConfigureUtil.configure(closure, acc.info);
        }
        for (Closure closure : proxyConfigure) {
            ConfigureUtil.configure(closure, acc.proxy);
        }

        // Ensure build name and number are not empty
        if (StringUtils.isBlank(acc.info.getBuildName())) {
            try {
                acc.info.setBuildName(URLEncoder.encode(getProject().getName(), "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                throw new GradleException("JDK does not have UTF-8 Encoding!", e);
            }
        }
        if (StringUtils.isBlank(acc.info.getBuildNumber())) {
            acc.info.setBuildNumber(acc.info.getBuildStarted());
        }

        // Populate the default matrix params
        acc.publisher.addMatrixParam(BuildInfoFields.BUILD_NAME, acc.info.getBuildName());
        acc.publisher.addMatrixParam(BuildInfoFields.BUILD_NUMBER, acc.info.getBuildNumber());
        acc.publisher.addMatrixParam(BuildInfoFields.BUILD_STARTED, acc.info.getBuildStarted());
        if (StringUtils.isNotBlank(acc.info.getVcsRevision())) {
            acc.publisher.addMatrixParam(BuildInfoFields.VCS_REVISION, acc.info.getVcsRevision());
        }
        if (StringUtils.isNotBlank(acc.info.getAgentName())) {
            acc.publisher.addMatrixParam(BuildInfoFields.AGENT_NAME, acc.info.getAgentName());
        }
        if (StringUtils.isNotBlank(acc.info.getAgentVersion())) {
            acc.publisher.addMatrixParam(BuildInfoFields.AGENT_VERSION, acc.info.getAgentVersion());
        }
    }

    public String getContextUrl() {
        return acc.getContextUrl();
    }

    public void setContextUrl(String contextUrl) {
        acc.setContextUrl(contextUrl);
    }

    public void setTimeout(Integer timeout) {
        acc.setTimeout(timeout);
    }

    public Integer getTimeout() {
        return acc.getTimeout();
    }

    public Boolean isIncludeEnvVars() {
        return acc.isIncludeEnvVars();
    }

    public void setIncludeEnvVars(Boolean enabled) {
        acc.setIncludeEnvVars(enabled);
    }

    public String getExportFile() {
        return acc.getExportFile();
    }

    public void setExportFile(String exportFile) {
        acc.setExportFile(exportFile);
    }

    public String getPropertiesFile() {
        return acc.getPropertiesFile();
    }

    public void setPropertiesFile(String propertyFile) {
        acc.setPropertiesFile(propertyFile);
    }

    public void resolver(Closure config) {
        if (resolverConfigure == null) {
            resolverConfigure = new ArrayList<Closure>();
        }
        resolverConfigure.add(config);
    }

    public void publisher(Closure config) {
        if (publisherConfigure == null) {
            publisherConfigure = new ArrayList<Closure>();
        }
        publisherConfigure.add(config);
    }

    public void info(Closure config) {
        if (infoConfigure == null) {
            infoConfigure = new ArrayList<Closure>();
        }
        infoConfigure.add(config);
    }

    public void proxy(Closure config) {
        if (proxyConfigure == null) {
            proxyConfigure = new ArrayList<Closure>();
        }
        proxyConfigure.add(config);
    }
}
