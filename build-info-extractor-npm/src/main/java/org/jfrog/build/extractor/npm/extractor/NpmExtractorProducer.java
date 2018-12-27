package org.jfrog.build.extractor.npm.extractor;

import org.apache.commons.lang.StringUtils;
import org.jfrog.build.extractor.npm.types.NpmPackageInfo;
import org.jfrog.build.extractor.producerConsumer.ProducerRunnableBase;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.Enumeration;

/**
 * Traverse over the dependencies tree of 'NpmPackageInfo's. If a node is legal - Produce it.
 *
 * @author Yahav Itzhak
 */
public class NpmExtractorProducer extends ProducerRunnableBase {

    private DefaultMutableTreeNode dependenciesRootNode;

    NpmExtractorProducer(DefaultMutableTreeNode dependenciesRootNode) {
        this.dependenciesRootNode = dependenciesRootNode;
    }

    @Override
    public void producerRun() throws InterruptedException {
        try {
            Enumeration e = dependenciesRootNode.breadthFirstEnumeration();
            while (e.hasMoreElements()) {
                if (Thread.interrupted()) {
                    break;
                }

                DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.nextElement();
                NpmPackageInfo npmPackageInfo = (NpmPackageInfo) node.getUserObject();
                if (npmPackageInfo == null) {
                    continue;
                }
                if (StringUtils.isBlank(npmPackageInfo.getVersion())) {
                    log.warn("npm dependencies list contains the package " + npmPackageInfo.getName() + " without version information. The dependency will not be added to build-info");
                    continue;
                }
                executor.put(npmPackageInfo);
            }
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            // Throw unchecked exception for the UncaughtExceptionHandler
            throw new RuntimeException(e);
        }
    }
}
