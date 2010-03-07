package org.jfrog.maven;

import org.apache.maven.execution.AbstractExecutionListener;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.ExecutionListener;

/**
 * @author Noam Y. Tenne
 */
public class ArtifactoryExecutionListener extends AbstractExecutionListener {

    private ExecutionListener listenerToWrap;

    public ArtifactoryExecutionListener(ExecutionListener listenerToWrap) {
        this.listenerToWrap = listenerToWrap;
    }

    public ArtifactoryExecutionListener() {
        listenerToWrap = null;
    }

    @Override
    public void projectDiscoveryStarted(ExecutionEvent event) {
        System.out.println("##################### projectDiscoveryStarted");
        if (listenerToWrap != null) {
            listenerToWrap.projectDiscoveryStarted(event);
        }
    }

    @Override
    public void sessionStarted(ExecutionEvent event) {
        System.out.println("##################### sessionStarted");
        if (listenerToWrap != null) {
            listenerToWrap.sessionStarted(event);
        }
    }

    @Override
    public void sessionEnded(ExecutionEvent event) {
        System.out.println("##################### sessionEnded");
        if (listenerToWrap != null) {
            listenerToWrap.sessionEnded(event);
        }
    }

    @Override
    public void projectSkipped(ExecutionEvent event) {
        System.out.println("##################### projectSkipped");
        if (listenerToWrap != null) {
            listenerToWrap.projectSkipped(event);
        }
    }

    @Override
    public void projectStarted(ExecutionEvent event) {
        System.out.println("##################### projectStarted");
        if (listenerToWrap != null) {
            listenerToWrap.projectStarted(event);
        }
    }

    @Override
    public void projectSucceeded(ExecutionEvent event) {
        System.out.println("##################### projectSucceeded");
        if (listenerToWrap != null) {
            listenerToWrap.projectSucceeded(event);
        }
    }

    @Override
    public void projectFailed(ExecutionEvent event) {
        System.out.println("##################### projectFailed");
        if (listenerToWrap != null) {
            listenerToWrap.projectFailed(event);
        }
    }

    @Override
    public void forkStarted(ExecutionEvent event) {
        System.out.println("##################### forkStarted");
        if (listenerToWrap != null) {
            listenerToWrap.forkStarted(event);
        }
    }

    @Override
    public void forkSucceeded(ExecutionEvent event) {
        System.out.println("##################### forkSucceeded");
        if (listenerToWrap != null) {
            listenerToWrap.forkSucceeded(event);
        }
    }

    @Override
    public void forkFailed(ExecutionEvent event) {
        System.out.println("##################### forkFailed");
        if (listenerToWrap != null) {
            listenerToWrap.forkFailed(event);
        }
    }

    @Override
    public void mojoSkipped(ExecutionEvent event) {
        System.out.println("##################### mojoSkipped");
        if (listenerToWrap != null) {
            listenerToWrap.mojoSkipped(event);
        }
    }

    @Override
    public void mojoStarted(ExecutionEvent event) {
        System.out.println("##################### mojoStarted");
        if (listenerToWrap != null) {
            listenerToWrap.mojoStarted(event);
        }
    }

    @Override
    public void mojoSucceeded(ExecutionEvent event) {
        System.out.println("##################### mojoSucceeded");
        if (listenerToWrap != null) {
            listenerToWrap.mojoSucceeded(event);
        }
    }

    @Override
    public void mojoFailed(ExecutionEvent event) {
        System.out.println("##################### mojoFailed");
        if (listenerToWrap != null) {
            listenerToWrap.mojoFailed(event);
        }
    }
}
