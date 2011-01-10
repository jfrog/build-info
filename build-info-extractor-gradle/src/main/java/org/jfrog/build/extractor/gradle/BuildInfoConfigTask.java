package org.jfrog.build.extractor.gradle;

import groovy.lang.Closure;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.util.ConfigureUtil;
import org.jfrog.build.ArtifactoryPluginUtils;
import org.jfrog.build.client.ArtifactoryClientConfiguration;
import org.jfrog.build.extractor.logger.GradleClientLogger;

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
        acc = new ArtifactoryClientConfiguration(new GradleClientLogger(getLogger()));
    }

    @TaskAction
    void fillClientConfiguration() {
        ArtifactoryClientConfiguration finalAcc = ArtifactoryPluginUtils.getArtifactoryClientConfiguration(getProject());
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
