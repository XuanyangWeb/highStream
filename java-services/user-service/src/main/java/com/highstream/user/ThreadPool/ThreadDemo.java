package com.highstream.user.ThreadPool;

import java.io.FileInputStream;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadDemo {

    /**
     * public ThreadPoolExecutor(int corePoolSize,
     * int maximumPoolSize,
     * long keepAliveTime,
     * TimeUnit unit,
     * BlockingQueue<Runnable> workQueue,
     * ThreadFactory threadFactory,
     * RejectedExecutionHandler handler) {
     * new Thread
     */

    ThreadPoolExecutor pool = new ThreadPoolExecutor(5, 10, 1, TimeUnit.SECONDS, new ArrayBlockingQueue<>(100), new ThreadPoolExecutor.CallerRunsPolicy());

    public void temst() {


        for (int i = 0; i < 5; i++) pool.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(2);

                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        pool.shutdown();
    }

}
