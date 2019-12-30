package com.shuai.dlog.excutor;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class DLogThreadPoolManager {
    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    /**
     * We want at least 2 threads and at most 4 threads in the core pool,
     * preferring to have 1 less than the CPU count to avoid saturating
     * the CPU with background work
     */
    private static final int CORE_POOL_SIZE = Math.max(2, Math.min(CPU_COUNT - 1, 4));
    private static final int MAXIMUM_POOL_SIZE = CPU_COUNT * 2 + 1;
    private static final int KEEP_ALIVE_SECONDS = 30;
    private static DLogThreadPoolManager threadPoolManager = new DLogThreadPoolManager();
    private final ThreadPoolExecutor executor;

    public static DLogThreadPoolManager getInstance() {
        return threadPoolManager;
    }

    private DLogThreadPoolManager() {
        ThreadFactory sThreadFactory = new ThreadFactory() {
            private final AtomicInteger mCount = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "ThreadPoolManager # thread_for_insert_statistics_to_db" + mCount.getAndIncrement());
            }
        };
        executor = new ThreadPoolExecutor(
                CORE_POOL_SIZE,
                MAXIMUM_POOL_SIZE,
                KEEP_ALIVE_SECONDS,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(128),
                sThreadFactory,
                new RejectedExecutionHandler() {
                    @Override
                    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                        //超出缓冲队列，被丢弃的任务，一般打点中并行执行的入库任务不会超过队列数量
                        r.run();
                    }
                });
    }

    public void executeThread(Runnable runnable) {
        if (runnable != null) {
            executor.execute(runnable);
        }
    }
}
