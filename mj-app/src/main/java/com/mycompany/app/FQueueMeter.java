package com.mycompany.app;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import com.mycompany.util.JUL;
import com.mycompany.util.RandomStringUtils;

import io.fqueue.FQueue;
import io.fqueue.FileFormatException;

public class FQueueMeter {
    static {
        JUL.initLogging();
    }

    public static void main(String[] args) throws IOException, FileFormatException, InterruptedException {
        FQueue fQueue = new FQueue("fqueue-meter", 1024 * 1024 * 10, 1024 * 1024);
        AtomicBoolean stopProducer = new AtomicBoolean();
        AtomicBoolean stopConsumer = new AtomicBoolean();
        AtomicLong bytesProducer = new AtomicLong();
        AtomicLong bytesConsumer = new AtomicLong();
        AtomicLong countProducer = new AtomicLong();
        AtomicLong countConsumer = new AtomicLong();
        Thread producer = Thread.ofPlatform().name("Producer").start(() -> {
            while (!stopProducer.get()) {
                String message = RandomStringUtils.random(
                        ThreadLocalRandom.current().nextInt(1 * 1024, 4 * 1024));
                byte[] data = message.getBytes();
                fQueue.add(data);
                countProducer.addAndGet(1);
                bytesProducer.addAndGet(data.length);
            }
        });
        Thread consumer = Thread.ofPlatform().name("Consumer").start(() -> {
            while (!stopConsumer.get()) {
                byte[] message = fQueue.poll();
                if (message == null) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                    }
                } else {
                    countConsumer.addAndGet(1);
                    bytesConsumer.addAndGet(message.length);
                }
            }
        });
        ScheduledExecutorService meter = new ScheduledThreadPoolExecutor(1);
        AtomicLong lastProducerBytes = new AtomicLong();
        AtomicLong lastConsumerBytes = new AtomicLong();
        AtomicLong lastProducerCount = new AtomicLong();
        AtomicLong lastConsumerCount = new AtomicLong();
        meter.scheduleAtFixedRate(() -> {
            long producerBytes = bytesProducer.get();
            long bpsProducer = producerBytes - lastProducerBytes.get();
            lastProducerBytes.set(producerBytes);

            long consumerBytes = bytesConsumer.get();
            long bpsConsumer = consumerBytes - lastConsumerBytes.get();
            lastConsumerBytes.set(consumerBytes);

            System.out.println("bps (producer) - " + bpsProducer + ", bps (consumer) - " + bpsConsumer);

            long producerCount = countProducer.get();
            long cpsProducer = producerCount - lastProducerCount.get();
            lastProducerCount.set(producerCount);

            long consumerCount = countConsumer.get();
            long cpsConsumer = consumerCount - lastConsumerCount.get();
            lastConsumerCount.set(consumerCount);

            System.out.println("cps (producer) - " + cpsProducer + ", cps (consumer) - " + cpsConsumer);
        }, 1, 1, TimeUnit.SECONDS);
        CountDownLatch ensureShutdown = new CountDownLatch(1);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            stopProducer.set(true);
            try {
                producer.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            stopConsumer.set(true);
            try {
                consumer.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            while (fQueue.poll() != null)
                ;
            fQueue.close();
            meter.close();
            ensureShutdown.countDown();
        }));
        ensureShutdown.await();
    }
}
