package org.jfrog.build.extractor.producerConsumer;

import org.jfrog.build.api.producerConsumer.ProducerConsumerItem;
import org.jfrog.build.api.util.Log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Producer-Consumer class for multi-threaded operation.
 * Provided ProducerRunnableBase and ConsumerRunnableBase objects, this class runs and manages the operation using a BlockingQueue as the bounded-buffer.
 *
 * Created by Bar Belity on 27/03/2018.
 */
public class ProducerConsumerExecutor {

    /**
     * BlockingQueue of jobs which are inserted by the producers and removed by the consumers
     */
    private BlockingQueue<ProducerConsumerItem> queue;
    private final Log log;
    private ProducerRunnableBase[] producerRunnables;
    private ConsumerRunnableBase[] consumerRunnables;
    public ProducerConsumerItem TERMINATE = new ProducerConsumerTerminateItem();

    private Thread[] producerThreads;
    private Thread[] consumerThreads;
    private int producersNumber;
    private int consumersNumber;
    private AtomicBoolean errorOccurred = new AtomicBoolean(false);
    private AtomicInteger producersFinished = new AtomicInteger(0);

    public ProducerConsumerExecutor(Log log, ProducerRunnableBase[] producerRunnables, ConsumerRunnableBase[] consumerRunnables, int queueSize) {
        this.log = log;
        this.producerRunnables = producerRunnables;
        this.consumerRunnables = consumerRunnables;
        this.queue = new ArrayBlockingQueue<>(queueSize);
        this.producersNumber = producerRunnables.length;
        this.consumersNumber = consumerRunnables.length;
        this.producerThreads = new Thread[producersNumber];
        this.consumerThreads = new Thread[consumersNumber];
    }

    public void start() throws Exception {
        Thread.UncaughtExceptionHandler exceptionHandler = new ProducerConsumerExceptionHandler();
        // Create producer threads
        for (int i = 0; i < producersNumber; i++) {
            producerThreads[i] = new Thread(producerRunnables[i]);
            initializeThread(producerThreads[i], producerRunnables[i], "producer_" + i, exceptionHandler);
        }

        // Create consumer threads
        for (int i = 0; i < consumersNumber; i++) {
            consumerThreads[i] = new Thread(consumerRunnables[i]);
            initializeThread(consumerThreads[i], consumerRunnables[i], "consumer_" + i, exceptionHandler);
        }

        // Start consumers and producers
        for (Thread consumer : consumerThreads) {
            consumer.start();
        }
        for (Thread producer : producerThreads) {
            producer.start();
        }

        try {
            // Wait for consumers and producers to finish
            for (Thread consumer : consumerThreads) {
                consumer.join();
            }
            for (Thread producer : producerThreads) {
                producer.join();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            stopWithException();
            throw e;
        }
        // Check if error occurred during deployment
        if (errorOccurred.get()) {
            throw new Exception("Error occurred during operation, please refer to logs for more information.");
        }
    }

    private void initializeThread(Thread thread, ProducerConsumerRunnableInt runnable, String name, Thread.UncaughtExceptionHandler exceptionHandler) {
        runnable.setExecutor(this);
        runnable.setLog(log);
        thread.setName(name);
        thread.setUncaughtExceptionHandler(exceptionHandler);
    }

    /**
     * This method will run when a producer completes its operation
     */
    public void producerFinished() throws InterruptedException {
        if (producersFinished.addAndGet(1) == producersNumber) {
            // Meaning all producers have finished
            queue.put(TERMINATE);
        }
    }

    /**
     * This method will run when an error occurred during execution
     */
    private void stopWithException() {
        // Interrupt all threads
        for (Thread thread : producerThreads) {
            thread.interrupt();
        }
        for (Thread thread : consumerThreads) {
            thread.interrupt();
        }
    }

    public void put(ProducerConsumerItem produced) throws InterruptedException {
        queue.put(produced);
    }

    public ProducerConsumerItem take() throws InterruptedException {
        return queue.take();
    }

    private class ProducerConsumerExceptionHandler implements Thread.UncaughtExceptionHandler {
        @Override
        public void uncaughtException(Thread t, Throwable e) {
            // Log the exception
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            log.error(String.format("[%s] An exception occurred during execution:\n%s", t.getName(), sw.toString()));

            // Stop all deployment operation if this is the first exception
            if (!errorOccurred.getAndSet(true)) {
                stopWithException();
            }
        }
    }

    /**
     * ProducerConsumerExecutor inner class, used to indicate termination of the ProducerConsumer operation.
     */
    private class ProducerConsumerTerminateItem implements ProducerConsumerItem {}
}
