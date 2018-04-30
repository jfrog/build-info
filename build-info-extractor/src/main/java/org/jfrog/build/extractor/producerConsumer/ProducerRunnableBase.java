package org.jfrog.build.extractor.producerConsumer;

/**
 * Base class for Producers used in the ProducerConsumerExecutor.
 *
 * Created by Bar Belity on 26/04/2018.
 */
public abstract class ProducerRunnableBase implements ProducerConsumerRunnableInt {

    protected ProducerConsumerExecutor executor;

    public final void run() {
        producerRun();
        try {
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
}
