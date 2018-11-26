package org.openas2.util;

import java.io.File;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class HighPerformanceBlockingQueue
{
    private final BlockingQueue<String> queue;

    private  final  int maxLimit;


    public HighPerformanceBlockingQueue(int maxLimit)
    {
        this.queue = new LinkedBlockingQueue<String>();

        this.maxLimit = maxLimit;

    }

    public  Boolean isEmpty() {
        synchronized (this.queue) {
            return this.queue.isEmpty();
        }
    }

    public  int size() {
        synchronized (this.queue) {
            return this.queue.size();
        }
    }

    public void AddPath(String msg)
    {
        synchronized (this.queue) {


            if (this.queue.size()<=maxLimit && !this.queue.contains(msg) ){

                this.queue.add(msg);
            }
        }
    }

    public String GetPath() throws Exception {

        synchronized (this.queue) {
            if(!this.queue.isEmpty()) {


                return this.queue.take();
            }
            else

            {
                return "";
            }
        }
    }
}