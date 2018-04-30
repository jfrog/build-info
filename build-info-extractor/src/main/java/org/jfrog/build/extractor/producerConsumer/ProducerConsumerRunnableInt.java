package org.jfrog.build.extractor.producerConsumer;

import org.jfrog.build.api.util.Log;

/**
 * Interface to use with the ProducerConsumerExecutor class.
 *
 * Created by Bar Belity on 27/03/2018.
 */
public interface ProducerConsumerRunnableInt extends Runnable {

    void setExecutor(ProducerConsumerExecutor executor);

    void setLog(Log log);
}
