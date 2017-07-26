package org.jfrog.gradle.plugin.artifactory.task;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.gradle.api.DefaultTask;
import org.gradle.api.Task;
import org.gradle.api.specs.Spec;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.jfrog.build.api.release.Distribution;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient;
import org.jfrog.gradle.plugin.artifactory.dsl.ArtifactoryPluginConvention;
import org.jfrog.gradle.plugin.artifactory.extractor.GradleClientLogger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class DistributeTask extends DefaultTask {

    @Input
    boolean publish = true;

    @Input
    boolean overrideExistingFiles = false;

    @Input
    boolean async = true;

    @Input
    @Optional
    String gpgPassphrase;

    @Input
    String targetRepo;

    @Input
    Set<String> sourceRepos = new HashSet<String>();

    @Input
    boolean dryRun = false;

    @OutputFile
    File cacheFile = new File(getProject().getBuildDir(), "distribute-task.txt");

    public DistributeTask() {
        onlyIf(new Spec<Task>() {
            @Override
            public boolean isSatisfiedBy(Task element) {
                return !sourceRepos.isEmpty();
            }
        });
    }

    @TaskAction
    public void distributeBuild() throws IOException {
        ArtifactoryPluginConvention convention = (ArtifactoryPluginConvention) getProject().getConvention().getPlugins().get("artifactory");
        ArtifactoryClientConfiguration clientConf = convention.getClientConfig();
        String buildNumber = clientConf.info.getBuildNumber();
        String buildName = clientConf.info.getBuildName();
        String contextUrl = clientConf.publisher.getContextUrl();
        String username = clientConf.publisher.getUsername();
        String password = clientConf.publisher.getPassword();

        ArtifactoryBuildInfoClient infoClient = new ArtifactoryBuildInfoClient(contextUrl, username, password, new GradleClientLogger(getLogger()));

        HttpResponse httpResponse = infoClient.distributeBuild(buildName, buildNumber, new Distribution(publish, overrideExistingFiles,
                gpgPassphrase, async, targetRepo, new ArrayList<String>(sourceRepos), false));

        InputStream content = httpResponse.getEntity().getContent();
        IOUtils.copy(content, new FileOutputStream(cacheFile));
    }

    public void sourceRepos(String repo) {
        sourceRepos.add(repo);
    }

    private static String booleanToString(boolean bool) {
        return bool ? "true" : "false";
    }

}
