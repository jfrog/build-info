package org.jfrog.build.config;

import org.apache.ivy.core.IvyContext;
import org.apache.ivy.core.event.publish.StartArtifactPublishEvent;
import org.apache.ivy.core.event.resolve.EndResolveEvent;
import org.apache.ivy.core.settings.IvySettings;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.jfrog.build.extractor.trigger.ArtifactoryBuildInfoTrigger;


/**
 * @author Tomer Cohen
 */
@Aspect
public class ArtifactoryIvySettingsConfigurator {

    @Pointcut("execution(* org.apache.ivy.Ivy.postConfigure(..))")
    public void interceptIvyPostConfigure() {
    }

    @Before("interceptIvyPostConfigure()")
    public void configure() throws Throwable {
        IvyContext context = IvyContext.getContext();
        IvySettings ivySettings = context.getSettings();
        ArtifactoryBuildInfoTrigger dependencyTrigger = new ArtifactoryBuildInfoTrigger();
        dependencyTrigger.setEvent(EndResolveEvent.NAME);
        ivySettings.addTrigger(dependencyTrigger);
        ArtifactoryBuildInfoTrigger publishTrigger = new ArtifactoryBuildInfoTrigger();
        publishTrigger.setEvent(StartArtifactPublishEvent.NAME);
        ivySettings.addTrigger(publishTrigger);
        context.getIvy().setSettings(ivySettings);
    }
}
