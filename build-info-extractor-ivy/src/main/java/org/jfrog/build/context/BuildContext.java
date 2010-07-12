package org.jfrog.build.context;

import com.google.common.collect.Sets;
import org.jfrog.build.api.Build;
import org.jfrog.build.client.DeployDetails;

import java.util.Set;


/**
 * Context container for Ivy builds, holds a set of {@link DeployDetails} for artifact to deploy after the build is
 * complete, as well as a final build-info object.
 *
 * @author Tomer Cohen
 */
public class BuildContext {
    /**
     * Name of the context that is being set as a useProperty.
     *
     * @see org.apache.tools.ant.PropertyHelper
     */
    public static String CONTEXT_NAME = "artifactory.ant.context";

    private Set<DeployDetails> deployDetails;
    private Build build;

    public BuildContext() {
        deployDetails = Sets.newHashSet();
    }

    public void addDeployDetailsForModule(DeployDetails deployDetails) {
        this.deployDetails.add(deployDetails);
    }

    public void setBuild(Build build) {
        this.build = build;
    }

    public Build getBuild() {
        return build;
    }

    public Set<DeployDetails> getDeployDetails() {
        return deployDetails;
    }
}
