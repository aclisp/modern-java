package com.mycompany.web;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShutdownHooks {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private Deque<Runnable> shutdownHooks = new ArrayDeque<Runnable>();
    private CountDownLatch ensureShutdown = new CountDownLatch(1);

    public void add(Runnable runOnShutdown) {
        shutdownHooks.push(runOnShutdown);
    }

    public void activate() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("About to shutdown...");
            shutdownHooks.forEach(hook -> hook.run());
            ensureShutdown.countDown();
        }, "ShutdownHooks"));
    }

    public void await() throws InterruptedException {
        ensureShutdown.await();
    }
}
