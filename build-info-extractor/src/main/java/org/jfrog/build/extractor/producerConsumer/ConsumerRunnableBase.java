package org.jfrog.build.extractor.producerConsumer;

/**
 * Base class for Consumers used in the ProducerConsumerExecutor.
 *
 * Created by Bar Belity on 26/04/2018.
 */
public abstract class ConsumerRunnableBase implements ProducerConsumerRunnableInt {

    public final void run() {
        consumerRun();
    }

    public abstract void consumerRun();
}
