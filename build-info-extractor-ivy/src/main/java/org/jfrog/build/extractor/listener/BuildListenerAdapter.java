package org.jfrog.build.extractor.listener;

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildListener;

/**
 * A convenience adapter class for build listener.
 *
 * @author Tomer Cohen
 */
public abstract class BuildListenerAdapter implements BuildListener {

    public void buildFinished(BuildEvent event) {
    }

    public void buildStarted(BuildEvent event) {
    }

    public void targetStarted(BuildEvent event) {
    }

    public void targetFinished(BuildEvent event) {
    }

    public void taskStarted(BuildEvent event) {
    }

    public void taskFinished(BuildEvent event) {
    }

    public void messageLogged(BuildEvent event) {
    }
}
