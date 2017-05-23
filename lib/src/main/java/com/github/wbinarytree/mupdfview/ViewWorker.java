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

    private Handler handler;
    private AtomicBoolean alive = new AtomicBoolean();
    private LinkedBlockingQueue<Task> queue;

    public ViewWorker() {
        handler = new Handler(Looper.getMainLooper());
        this.queue = new LinkedBlockingQueue<>();
    }

    public void addTask(Task task) {
        queue.add(task);
    }

    public void stop() {
        alive.compareAndSet(true, false);
        queue.clear();
        handler = null;
    }

    public void start() {
        if(alive.compareAndSet(false, true)){
            new Thread(this).start();
        }else {
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
                Task task = queue.take();
                task.work();
                runOnUiThread(task);
            } catch (Throwable x) {
                Log.e("ViewWorker", x.getMessage());
            }
        }
    }

    interface Task extends Runnable {
        void work();
    }
}
