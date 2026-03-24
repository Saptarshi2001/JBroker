
package com.jbroker;

import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;


class Worker implements Runnable {

    private BlockingQueue<Runnable> queue = null;

    Worker(BlockingQueue<Runnable> queue) {
        this.queue = queue;

    }

    @Override
    public void run() {

        try {
            while (!Thread.currentThread().isInterrupted()) {
                Runnable task = this.queue.take();
                task.run();
            }
        } catch (InterruptedException ex) {
            
            Logger.getLogger(Worker.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
    
    public void stop()
    {
        
    }

}
