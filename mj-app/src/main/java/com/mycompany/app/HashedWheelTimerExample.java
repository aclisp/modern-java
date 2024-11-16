package com.mycompany.app;

import static java.time.LocalDateTime.now;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.time.format.DateTimeFormatter;
import java.util.concurrent.CountDownLatch;

import io.netty.util.HashedWheelTimer;

public class HashedWheelTimerExample {

    private static DateTimeFormatter timef = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    public static void main(String[] args) throws InterruptedException {
        var latch = new CountDownLatch(1);

        // 初始化 HashedWheelTimer，设置1秒钟转动一次轮盘，轮盘的大小为8
        var timer = new HashedWheelTimer(
                /* tickDuration */1, SECONDS,
                /* ticksPerWheel */8);

        // 添加任务，设置 3 秒后执行
        System.out.println("任务添加时间: " + timef.format(now()));
        timer.newTimeout(timeout -> {
            System.out.println("任务执行: " + timef.format(now()));
            latch.countDown();
        }, /* delay */3, SECONDS);

        // 主线程等待，以便观察任务执行
        latch.await();
        // 停止定时器并释放资源
        timer.stop();
    }
}
