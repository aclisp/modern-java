package com.mycompany.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.prometheus.metrics.core.datapoints.CounterDataPoint;
import io.prometheus.metrics.core.datapoints.DistributionDataPoint;
import io.prometheus.metrics.core.metrics.Counter;
import io.prometheus.metrics.core.metrics.Summary;
import io.prometheus.metrics.model.registry.PrometheusRegistry;

// 来自https://www.baeldung.com/java-smart-batching
// 但结合两种Batcher并做了优化。
public class SmartBatcher<T, Q extends Queue<T>> {
    private final static Logger logger = LoggerFactory.getLogger(SmartBatcher.class);

    String name;
    Q tasksQueue;
    Consumer<Q> queueCloser;
    // Queue<T> tasksQueue = new ConcurrentLinkedQueue<>(); // 采用无锁队列
    // FQueue tasksQueue; // 采用文件队列
    int tasksQueueLengthMax;
    Thread batchThread; // 后台处理批量任务的线程
    int executionThreshold;
    int timeoutThreshold;
    int capacity;
    int busyWaitingGranularity = 100; // 轮询最小时间粒度(ms)
    volatile boolean working = false; // 是否正在执行批量任务
    volatile boolean shuttingDown = false; // 是否正在终止

    private final static Summary submitDelay = Summary.builder()
            .name("smart_batcher_tasks_submit_seconds")
            .help("SmartBatcher提交任务的延迟秒数")
            .labelNames("name")
            .quantile(0.5)
            .quantile(0.95)
            .quantile(1.0)
            .register(PrometheusRegistry.defaultRegistry);
    private final DistributionDataPoint submitDelayNamed;
    private final static Summary tasksQueueLength = Summary.builder()
            .name("smart_batcher_tasks_queue_length")
            .help("SmartBatcher任务聚合批次处理的队列长度")
            .labelNames("name")
            .quantile(0.5)
            .quantile(0.95)
            .quantile(1.0)
            .register(PrometheusRegistry.defaultRegistry);
    private final DistributionDataPoint tasksQueueLengthNamed;
    private final static Counter tasksIgnored = Counter.builder()
            .name("smart_batcher_tasks_ignored")
            .help("SmartBatcher丢弃的任务数量")
            .labelNames("name")
            .register(PrometheusRegistry.defaultRegistry);
    private final CounterDataPoint tasksIgnoredNamed;

    /**
     * Create a SmartBatcher instance.
     *
     * @param name               give me a name, so that the background thread can
     *                           be identified.
     * @param executionThreshold the number of tasks that must be buffered before we
     *                           execute them.
     *                           每批次任务数量的上限，取值应该接近处理器的带宽。
     * @param timeoutThreshold   the maximum amount of milliseconds we'll wait for
     *                           tasks to be buffered
     *                           before we process them, even if executionThreshold
     *                           hasn't been reached.
     *                           如果>0,比如1000,则任务最多延迟1秒才发；如果<=0,则任务无延迟立刻发。
     * @param capacity           the capacity of task queue. 超过会丢弃任务。
     * @param executionLogic     the batch handler which is executed in the
     *                           background thread
     */
    public SmartBatcher(String name,
            int executionThreshold,
            int timeoutThreshold,
            int capacity,
            Supplier<Q> queueSupplier,
            Consumer<Q> queueCloser,
            Consumer<List<T>> executionLogic) {
        if (executionThreshold <= 0) {
            throw new IllegalArgumentException("maxBatchCount必须>0");
        }
        batchThread = new Thread(batchHandling(executionLogic));
        batchThread.setDaemon(true); // 设成精灵，防止它阻止JVM结束
        batchThread.setName("SmartBatcher-" + name);
        this.executionThreshold = executionThreshold; // 一批不超过多少个
        this.timeoutThreshold = timeoutThreshold; // 时间到了，这一批不够数也发
        this.capacity = capacity;
        this.name = name;
        this.tasksQueue = queueSupplier.get();
        if (this.tasksQueue == null) {
            throw new IllegalArgumentException("Can not create tasksQueue");
        }
        this.queueCloser = queueCloser;

        submitDelayNamed = submitDelay.labelValues(this.name);
        tasksQueueLengthNamed = tasksQueueLength.labelValues(this.name);
        tasksIgnoredNamed = tasksIgnored.labelValues(this.name);

        batchThread.start();
    }

    public void submit(T task) { // 绝不会阻塞
        if (shuttingDown) {
            logger.error("SmartBatcher-{}正在关闭，此任务将丢弃:task={}", name, task);
            tasksIgnoredNamed.inc(1);
            return;
        }
        long start = System.currentTimeMillis();
        int queueSize = tasksQueue.size();
        if (queueSize > capacity) {
            tasksIgnoredNamed.inc(1);
        } else {
            tasksQueue.add(task);
            queueSize += 1;
        }
        tasksQueueLengthNamed.observe(queueSize);
        if (queueSize > tasksQueueLengthMax) {
            tasksQueueLengthMax = queueSize;
        }
        submitDelayNamed.observe((System.currentTimeMillis() - start) / 1000.0);
    }

    public void shutdown() {
        batchThread.interrupt();
        try {
            batchThread.join();
        } catch (InterruptedException e) {
            logger.error("SmartBatcher-{}关闭流程被中断，可能有任务被丢弃", name);
        }
    }

    private Runnable batchHandling(Consumer<List<T>> executionLogic) {
        return () -> {
            logger.info("SmartBatcher线程{}正在后台帮您聚沙成塔:maxBatchCount={},lingerMillis={}", name, executionThreshold,
                    timeoutThreshold);
            loop: // 核心智能批处理
            while (!batchThread.isInterrupted()) {
                long startTime = System.currentTimeMillis();
                while (tasksQueue.size() < executionThreshold
                        && (System.currentTimeMillis() - startTime) < timeoutThreshold) {
                    try {
                        Thread.sleep(busyWaitingGranularity);
                    } catch (InterruptedException e) {
                        break loop;
                    }
                }

                List<T> tasks = new ArrayList<>(executionThreshold);
                int transferred = drainTo(tasksQueue, tasks, executionThreshold);
                if (transferred > 0) {
                    doWork(tasks, executionLogic);
                } else if (timeoutThreshold <= 0) {
                    try {
                        Thread.sleep(busyWaitingGranularity);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }

            // 线程退出，清理积压任务
            logger.info("SmartBatcher线程{}正在退出并清理积压任务", name);
            shuttingDown = true; // 先禁止任务提交
            int transferred;
            do {
                List<T> tasks = new ArrayList<>(executionThreshold);
                transferred = drainTo(tasksQueue, tasks, executionThreshold);
                if (transferred > 0) {
                    doWork(tasks, executionLogic);
                }
            } while (transferred > 0);
            if (queueCloser != null) {
                queueCloser.accept(tasksQueue);
            }
            logger.info("SmartBatcher线程{}退出完毕", name);
        };
    }

    private void doWork(List<T> tasks, Consumer<List<T>> executionLogic) {
        working = true;
        try {
            executionLogic.accept(tasks);
        } catch (Throwable ex) {
            logger.error("SmartBatcher线程{}批次处理抛出异常", name, ex);
        }
        working = false;
    }

    /**
     * Removes at most the given number of available elements from
     * this queue and adds them to the given collection.
     *
     * @param q   the queue to transfer elements from
     * @param c   the collection to transfer elements into
     * @param cap the collection's capacity
     * @return the number of elements transferred
     */
    private int drainTo(Q q, Collection<T> c, int cap) {
        int n = 0;
        T t;
        while (c.size() < cap && (t = q.poll()) != null) {
            c.add(t);
            n++;
        }
        return n;
    }

}
