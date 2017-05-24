package com.github.wbinarytree.mupdfview;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by yaoda on 02/05/17.
 */

public class ViewWorker implements Runnable {

    private final LinkedBlockingQueue<Task> queue;
    private Handler handler;
    private AtomicBoolean alive = new AtomicBoolean();

    public ViewWorker() {
        handler = new Handler(Looper.getMainLooper());
        this.queue = new LinkedBlockingQueue<>();
    }

    public void addTask(Task task) {
        queue.add(task);
    }

    public void stop() {
        alive.compareAndSet(true, false);
        clear();
        handler = null;
    }

    public void restart() {
        clear();
    }

    private void clear() {
        synchronized (queue) {
            while (!queue.isEmpty()) {
                Task task = queue.peek();
                if (task != null) {
                    task.stop();
                    queue.remove(task);
                }
            }
        }
    }

    public void start() {
        if (alive.compareAndSet(false, true)) {
            new Thread(this).start();
        } else {
            throw new IllegalStateException("Worker already started");
        }
    }

    public boolean isWorking() {
        return alive.get();
    }

    private void runOnUiThread(Runnable r) {
        handler.post(r);
    }

    @Override
    public void run() {
        while (alive.get()) {
            try {
                Task task = queue.peek();
                if (task == null) continue;
                task.startWorking();
                if (task.isWorking()) {
                    task.work();
                }
                if (task.isWorking()) {
                    runOnUiThread(task);
                }
                queue.remove(task);
            } catch (Throwable x) {
                Log.e("ViewWorker", x.getMessage());
            }
        }
    }

    abstract static class Task implements Runnable {
        private AtomicBoolean working = new AtomicBoolean();

        abstract void work();

        final void startWorking() {
            if (!this.working.compareAndSet(false, true)) {
                throw new IllegalStateException("Task already started");
            }
        }

        final boolean isWorking() {
            return working.get();
        }

        final void stop() {
            this.working.set(false);
        }
    }
}
