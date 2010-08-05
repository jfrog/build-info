package org.jfrog.build.config;

import org.apache.tools.ant.Project;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.jfrog.build.extractor.listener.ArtifactoryBuildListener;

/**
 * @author Noam Y. Tenne
 */
@Aspect
public class ArtifactoryIvyListenerConfigurator {

    @Pointcut("execution(* org.apache.tools.ant.Main.addBuildListeners(..)) && args(project)")
    public void interceptAddAntBuildListener(Project project) {
    }

    @Before(value = "interceptAddAntBuildListener(project)", argNames = "project")
    public void addBuildListener(Project project) throws Throwable {
        project.addBuildListener(new ArtifactoryBuildListener());
    }
}
