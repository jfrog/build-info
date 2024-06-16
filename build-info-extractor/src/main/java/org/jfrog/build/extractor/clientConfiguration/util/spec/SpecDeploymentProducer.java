package org.jfrog.build.extractor.clientConfiguration.util.spec;

import org.jfrog.build.api.multiMap.Multimap;
import org.jfrog.build.extractor.clientConfiguration.deploy.DeployDetails;
import org.jfrog.build.extractor.producerConsumer.ProducerRunnableBase;
import org.jfrog.filespecs.FileSpec;
import org.jfrog.filespecs.entities.FilesGroup;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * Producer object to use with the ProducerConsumerExecutor during artifact deployment by filespec operation.
 * Created by Bar Belity on 27/03/2018.
 */
public class SpecDeploymentProducer extends ProducerRunnableBase {

    /**
     * Set containing all created DeployDetails, used to later create deployed Artifact objects
     */
    private Set<DeployDetails> deployDetailsSet = new HashSet<>();

    private FileSpec spec;
    private File workspace;
    private Multimap<String, String> buildProperties;

    SpecDeploymentProducer(FileSpec spec, File workspace, Multimap<String, String> buildProperties) {
        this.spec = spec;
        this.workspace = workspace;
        this.buildProperties = buildProperties;
    }

    @Override
    public void producerRun() throws InterruptedException {
        log.debug(String.format("[Thread %s] starting run()", Thread.currentThread().getName()));
        try {
            // Iterate over FileSpecs
            for (FilesGroup uploadFile : spec.getFiles()) {
                if (Thread.interrupted()) {
                    break;
                }

                log.debug(String.format("[Thread %s] getting deploy details from the following json: \n %s ", Thread.currentThread().getName(), uploadFile.toString()));

                // Execute FileSpec
                SingleSpecDeploymentProducer fileSpecProducer = new SingleSpecDeploymentProducer(uploadFile, workspace, buildProperties);
                fileSpecProducer.executeSpec(deployDetailsSet, executor);
            }
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            // Throw unchecked exception for the UncaughtExceptionHandler
            throw new RuntimeException(e);
        }
    }

    /**
     * Return the DeployDetails created during producer run
     */
    public Set<DeployDetails> getDeployedArtifacts() {
        return deployDetailsSet;
    }
}