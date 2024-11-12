package com.mycompany.app;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

import com.mycompany.util.JUL;

public class ThreadPoolTaskSchedulerExample {
    static {
        JUL.initLogging();
    }
    private final static Logger logger = LoggerFactory.getLogger(ThreadPoolTaskSchedulerExample.class);
    private final static DateTimeFormatter timef = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    static String localDateTimeToCron(LocalDateTime dt) {
        return dt.getSecond() + " " +
                dt.getMinute() + " " +
                dt.getHour() + " " +
                dt.getDayOfMonth() + " " +
                dt.getMonth().getValue() + " ?";
    }

    public static void main(String[] args) throws InterruptedException {
        var latch = new CountDownLatch(1);
        var scheduler = new ThreadPoolTaskScheduler();
        scheduler.initialize();
        scheduler.schedule(() -> {
            logger.info("每 5 秒执行: " + timef.format(LocalDateTime.now()));
        }, new CronTrigger("*/5 * * * * *"));
        scheduler.schedule(() -> {
            logger.info("执行任务: " + timef.format(LocalDateTime.now()));
        }, new CronTrigger(localDateTimeToCron(LocalDateTime.now().plus(Duration.ofSeconds(8)))));
        latch.await();
    }
}
