package org.jfrog.build.context;

import org.apache.ivy.core.IvyContext;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.api.Module;
import org.jfrog.build.client.DeployDetails;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * Context container for Ivy builds, holds a set of {@link DeployDetails} for artifact to deploy after the build is
 * complete, as well as a final build-info object.
 *
 * @author Tomer Cohen
 */
public class BuildContext {
    /**
     * Name of the context that is being in the contextMap of {@link IvyContext}.
     */
    public static final String CONTEXT_NAME = "artifactory.ant.context";

    private Set<DeployDetails> deployDetails;
    private List<Module> modules;
    private List<Dependency> dependencies;
    private long buildStartTime;

    public BuildContext() {
        deployDetails = new HashSet<DeployDetails>();
        modules = new ArrayList<Module>();
        dependencies = new ArrayList<Dependency>();
        buildStartTime = System.currentTimeMillis();
    }

    public void addDeployDetailsForModule(DeployDetails deployDetails) {
        this.deployDetails.add(deployDetails);
    }

    public void addModule(Module module) {
        this.modules.add(module);
    }

    public List<Module> getModules() {
        return modules;
    }

    public Set<DeployDetails> getDeployDetails() {
        return deployDetails;
    }

    public void addDependency(Dependency dependency) {
        this.dependencies.add(dependency);
    }

    public List<Dependency> getDependencies() {
        return dependencies;
    }

    public long getBuildStartTime() {
        return buildStartTime;
    }

    public void setBuildStartTime(long buildStartTime) {
        this.buildStartTime = buildStartTime;
    }
}
