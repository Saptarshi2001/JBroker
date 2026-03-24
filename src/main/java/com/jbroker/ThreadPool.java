
package com.jbroker;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Logger;


public class ThreadPool {

    private BlockingQueue<Runnable> queue = null;
    private List<Worker> workers = new ArrayList<>();
    private boolean isstopped = false;
    private Logger logger = Logger.getLogger("threadpoolissues.log");
    public ThreadPool(int numberofthreads, int maxnooftasks) {
        queue = new ArrayBlockingQueue<>(maxnooftasks);
        for (int i = 0; i < numberofthreads; i++) {
            Worker worker = new Worker(queue);
            workers.add(worker);
        }
        for (Worker work : workers) {
            new Thread(work).start();
        }
    }

    public void execute(Runnable task)throws InterruptedException{
       try{
        this.queue.put(task);
       }catch(InterruptedException ex){
        logger.info(ex.getMessage());
       }
    }

    public void stop() {
        this.isstopped = true;
        for (Worker worker : workers) {
            worker.stop();
        }
    }

}
