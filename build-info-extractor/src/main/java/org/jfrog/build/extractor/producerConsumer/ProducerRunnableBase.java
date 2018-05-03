package org.jfrog.build.extractor.producerConsumer;

import org.jfrog.build.api.util.Log;

/**
 * Base class for Producers used in the ProducerConsumerExecutor.
 *
 * Created by Bar Belity on 26/04/2018.
 */
public abstract class ProducerRunnableBase implements ProducerConsumerRunnableInt {

    protected ProducerConsumerExecutor executor;
    protected Log log;

    public final void run() {
        try {
            producerRun();
            executor.producerFinished();
        } catch (InterruptedException e) {
            return;
        }
    }

    public abstract void producerRun();

    @Override
    public void setExecutor(ProducerConsumerExecutor executor) {
        this.executor = executor;
    }

    @Override
    public void setLog(Log log) {
        this.log = log;
    }
}
