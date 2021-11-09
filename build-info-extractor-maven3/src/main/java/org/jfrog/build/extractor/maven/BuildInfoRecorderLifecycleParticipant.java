package org.jfrog.build.extractor.maven;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.ExecutionListener;
import org.apache.maven.execution.MavenSession;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.jfrog.build.api.BuildInfoConfigProperties;
import org.jfrog.build.extractor.BuildInfoExtractorUtils;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration;
import org.jfrog.build.extractor.clientConfiguration.ClientProperties;

import java.util.Properties;

/**
 * @author Noam Y. Tenne
 */
@Component(role = AbstractMavenLifecycleParticipant.class)
public class BuildInfoRecorderLifecycleParticipant extends AbstractMavenLifecycleParticipant {

    @Requirement(role = BuildInfoRecorder.class, hint = "default", optional = false)
    BuildInfoRecorder recorder;
    @Requirement
    private Logger logger;
    private ArtifactoryClientConfiguration internalConfiguration = null;

    @Override
    public void afterProjectsRead(MavenSession session) throws MavenExecutionException {
        ArtifactoryClientConfiguration configuration = getConfiguration(session);
        Object activateRecorderObject = configuration.isActivateRecorder();
        if (activateRecorderObject == null) {
            logger.debug("Disabling Artifactory Maven3 Build-Info Recorder: activation property (" +
                    BuildInfoConfigProperties.ACTIVATE_RECORDER + ") not found.");
            return;
        }
        if (!Boolean.valueOf(activateRecorderObject.toString())) {
            logger.debug("Disabling Artifactory Maven3 Build-Info Recorder: activation property (" +
                    BuildInfoConfigProperties.ACTIVATE_RECORDER + ") value is either false or invalid.");
            return;
        }
        logger.debug("Activating Artifactory Maven3 Build-Info Recorder: activation property (" +
                BuildInfoConfigProperties.ACTIVATE_RECORDER + ") value is true.");
        configuration.info.setBuildStarted(System.currentTimeMillis());
        ExecutionListener existingExecutionListener = session.getRequest().getExecutionListener();
        recorder.setListenerToWrap(existingExecutionListener);
        recorder.setConfiguration(configuration);
        session.getRequest().setExecutionListener(recorder);
    }

    private ArtifactoryClientConfiguration getConfiguration(MavenSession session) {
        if (internalConfiguration != null) {
            return internalConfiguration;
        }
        Properties allMavenProps = new Properties();
        allMavenProps.putAll(session.getSystemProperties());
        allMavenProps.putAll(session.getUserProperties());

        Maven3BuildInfoLogger log = new Maven3BuildInfoLogger(logger);
        Properties allProps = BuildInfoExtractorUtils.mergePropertiesWithSystemAndPropertyFile(allMavenProps, log);
        if (allProps.getProperty(ClientProperties.PROP_INSECURE_TLS, "false").equals("true")) {
            System.setProperty("maven.wagon.http.ssl.insecure", "true");
            System.setProperty("maven.wagon.http.ssl.allowall", "true");
            System.setProperty("maven.wagon.http.ssl.ignore.validity.dates", "true");
        }
        internalConfiguration = new ArtifactoryClientConfiguration(log);
        internalConfiguration.fillFromProperties(allProps);
        return internalConfiguration;
    }
}
