package ru.michael1297;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

public class ThreadPool implements AutoCloseable {
    // need to keep track of threads, so we can join them
    private final List<Thread> workers;

    // the task queue
    private final Queue<Runnable> tasks;

    // synchronization
    private final Object queueMutex;
    private volatile boolean stop;

    /**
     * The constructor just launches some amount of workers
     * @param threads number of threads
     */
    @SuppressWarnings("ConstantConditions")
    public ThreadPool(int threads) {
        workers = new ArrayList<>();
        tasks = new LinkedList<>();
        queueMutex = new Object();
        stop = false;

        for (int i = 0; i < threads; i++) {
            workers.add(new Thread(() -> {
                while (true) {
                    Runnable task;
                    synchronized (queueMutex) {
                        while (!stop && tasks.isEmpty()) {
                            try {
                                queueMutex.wait();
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                return;
                            }
                        }
                        if (stop && tasks.isEmpty())
                            return;
                        task = tasks.poll();
                    }
                    task.run();
                }
            }));
        }
        workers.forEach(Thread::start);
    }

    /**
     * Add new work item to the pool
     * @param task work item
     * @return {@link Future<T>}
     * @param <T> The result type returned by this Future's get method
     * @Throws IllegalStateException if stopped ThreadPool
     */
    public <T> Future<T> enqueue(Callable<T> task) {
        FutureTask<T> future = new FutureTask<>(task);
        synchronized (queueMutex) {
            // don't allow enqueueing after stopping the pool
            if (stop) {
                throw new IllegalStateException("enqueue on stopped ThreadPool");
            }
            tasks.offer(future);
            queueMutex.notify();
        }
        return future;
    }

    /**
     * Join all threads, stop ThreadPool
     */
    @Override
    public void close() {
        synchronized (queueMutex) {
            stop = true;
            queueMutex.notifyAll();
        }
        for (Thread worker : workers) {
            try {
                worker.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
