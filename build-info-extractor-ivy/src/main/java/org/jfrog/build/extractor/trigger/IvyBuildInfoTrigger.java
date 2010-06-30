package org.jfrog.build.extractor.trigger;

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
import org.jfrog.build.extractor.BuildInfoExtractorUtils;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * This trigger is fired as a {@code post-publish-artifact} event. After the artifact has successfully been published to
 * either a remote or local repository, this event is fired. This is called after <b>every</b> publish event, if we have
 * 2 artifacts (jar, ivy) to be published, this event will be called 2 times.
 *
 * @author Tomer Cohen
 */
public class IvyBuildInfoTrigger extends AbstractTrigger implements Trigger {
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
        File baseDir = ivySettings.getBaseDir();
        File exportedBuildInfoFile = new File(baseDir, "build-info.json");
        try {
            BuildInfoExtractorUtils.saveBuildInfoToFile(build, exportedBuildInfoFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
