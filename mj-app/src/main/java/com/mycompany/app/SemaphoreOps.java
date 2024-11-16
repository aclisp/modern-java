package com.mycompany.app;

import java.util.concurrent.Semaphore;
import java.util.logging.Logger;

import com.mycompany.util.JUL;

public class SemaphoreOps {

    static {
        JUL.initLogging();
    }
    private static final Logger LOGGER = Logger.getLogger(Semaphore.class.getName());

    static void wait(Semaphore semaphore) {
        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            LOGGER.info(e.toString());
        }
    }

    static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            LOGGER.info(e.toString());
        }
    }

    public static void main(String[] args) {
        Semaphore startSubThread = new Semaphore(0);
        Semaphore endSubThread = new Semaphore(-1);

        LOGGER.info("start");
        Thread thread1 = new Thread(() -> {
            LOGGER.info("start thread1");
            wait(startSubThread);
            LOGGER.info("end thread1");
            endSubThread.release();
        }, "thread1");
        Thread thread2 = new Thread(() -> {
            LOGGER.info("start thread2");
            wait(startSubThread);
            LOGGER.info("end thread2");
            endSubThread.release();
        }, "thread2");
        thread1.start();
        thread2.start();

        sleep(5000);

        startSubThread.release(2);
        wait(endSubThread);
        LOGGER.info("end");
    }
}
