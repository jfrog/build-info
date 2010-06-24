package org.jfrog.build.extractor.listener;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.IvyContext;
import org.apache.ivy.core.event.IvyEvent;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.core.settings.IvyVariableContainer;
import org.apache.ivy.plugins.trigger.AbstractTrigger;
import org.apache.ivy.plugins.trigger.Trigger;
import org.jfrog.build.api.Agent;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.BuildAgent;
import org.jfrog.build.api.BuildType;
import org.jfrog.build.api.Module;
import org.jfrog.build.api.builder.BuildInfoBuilder;
import org.jfrog.build.client.ArtifactoryBuildInfoClient;
import org.jfrog.build.extractor.BuildInfoExtractor;
import org.jfrog.build.extractor.BuildInfoExtractorSpec;

import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * @author Tomer Cohen
 */
public class IvyBuildInfoTrigger extends AbstractTrigger implements Trigger, BuildInfoExtractor<IvyEvent, Build> {
    private Build build;


    public void progress(IvyEvent event) {
        Ivy ivy = IvyContext.getContext().getIvy();
        IvySettings ivySettings = ivy.getSettings();
        IvyVariableContainer ivyVariableContainer = ivySettings.getVariables();
        String projectName = ivyVariableContainer.getVariable("ant.project.name");
        BuildInfoBuilder builder = new BuildInfoBuilder(projectName);
        String ivyRevision = ivyVariableContainer.getVariable("ivy.new.revision");
        builder.version(ivyRevision);
        List<Module> modules = IvyModuleTrigger.getAllModules();
        builder.modules(modules);
        builder.properties(System.getProperties());
        builder.buildAgent(new BuildAgent("Ivy", Ivy.getIvyVersion()));
        builder.agent(new Agent("Ivy", Ivy.getIvyVersion()));
        builder.durationMillis(0L);
        builder.number("0");
        builder.startedDate(new Date());
        builder.type(BuildType.IVY);
        build = builder.build();
        ArtifactoryBuildInfoClient client =
                new ArtifactoryBuildInfoClient("http://localhost:8080/artifactory", "admin", "password");
        try {
            client.sendBuildInfo(build);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Build extract(IvyEvent context, BuildInfoExtractorSpec spec) {
        progress(context);
        return build;
    }
}
