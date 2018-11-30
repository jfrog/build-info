package org.jfrog.build.extractor.npm.extractor;

import org.apache.commons.lang.StringUtils;
import org.jfrog.build.api.PackageInfo;
import org.jfrog.build.extractor.producerConsumer.ProducerRunnableBase;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.Enumeration;

/**
 * Produces PackageInfos from the dependencies tree.
 *
 * Created by Yahav Itzhak on 25 Nov 2018.
 */
public class NpmExtractProducer extends ProducerRunnableBase {

    private DefaultMutableTreeNode dependenciesRootNode;

    NpmExtractProducer(DefaultMutableTreeNode dependenciesRootNode) {
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
                PackageInfo packageInfo = (PackageInfo) node.getUserObject();
                if (packageInfo == null) {
                    continue;
                }
                if (StringUtils.isBlank(packageInfo.getVersion())) {
                    log.warn("npm dependencies list contains the package " + packageInfo.getName() + " without version information. The dependency will not be added to build-info");
                    continue;
                }
                executor.put(packageInfo);
            }
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            // Throw unchecked exception for the UncaughtExceptionHandler
            throw new RuntimeException(e);
        }
    }
}
