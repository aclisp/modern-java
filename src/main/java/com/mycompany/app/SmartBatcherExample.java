package com.mycompany.app;

import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mycompany.util.JUL;
import com.mycompany.util.RandomStringUtils;
import com.mycompany.util.SmartBatcher;

import fqueue.FQueue;
import fqueue.FileFormatException;

public class SmartBatcherExample {
    static {
        JUL.initLogging();
    }
    private final static Logger logger = LoggerFactory.getLogger(SmartBatcherExample.class);

    public static void main(String[] args) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(2);
        SmartBatcher<byte[], FQueue> fQueueBatcher = new SmartBatcher<>(
                "FQueueBatcher",
                150,
                1000,
                10000,
                () -> {
                    try {
                        return new FQueue("fqueue-batcher", 1024 * 1024 * 10, 1024 * 1024);
                    } catch (IOException | FileFormatException e) {
                        e.printStackTrace();
                        return null;
                    }
                }, FQueue::close, items -> {
                    logger.info("fQueueBatcher items count - " + items.size());
                });
        SmartBatcher<String, ConcurrentLinkedQueue<String>> memBatcher = new SmartBatcher<>(
                "MemoryBatcher",
                120,
                1000,
                10000,
                ConcurrentLinkedQueue::new, null,
                items -> {
                    logger.info("memBatcher items count - " + items.size());
                });
        Thread.ofPlatform().name("fQueueBatcher Producer").start(() -> {
            for (int i = 0; i < 1000; i++) {
                String message = RandomStringUtils.random(
                        ThreadLocalRandom.current().nextInt(1 * 1024, 4 * 1024));
                fQueueBatcher.submit(message.getBytes());
            }
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
            }
            fQueueBatcher.submit("hello fQueueBatcher".getBytes());
            latch.countDown();
        });
        Thread.ofPlatform().name("memBatcher Producer").start(() -> {
            for (int i = 0; i < 1000; i++) {
                String message = RandomStringUtils.random(
                        ThreadLocalRandom.current().nextInt(1 * 1024, 4 * 1024));
                memBatcher.submit(message);
            }
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
            }
            memBatcher.submit("hello memBatcher");
            latch.countDown();
        });
        latch.await();
        // Thread.sleep(5000);
        memBatcher.shutdown();
        fQueueBatcher.shutdown();
    }
}
