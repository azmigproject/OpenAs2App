
package org.openas2.processor.receiver;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.joda.time.DateTime;
import org.openas2.OpenAS2Exception;
import org.openas2.Session;
import org.openas2.logging.Logger;
import org.openas2.params.InvalidParameterException;
import org.openas2.util.ThreadCleanUP;


public abstract class PollingModule extends MessageBuilderModule {
    private static final String PARAM_POLLING_INTERVAL = "interval";
    private Timer timer;
    private boolean busy;

    private Timer producerTimer;
    private boolean producerBusy;

    private Timer consumerTimer;
    private boolean consumerBusy;


    public void init(Session session, Map<String, String> options) throws OpenAS2Exception
    {
        super.init(session, options);
        getParameter(PARAM_POLLING_INTERVAL, true);

    }

    private int getInterval() throws InvalidParameterException
    {
       //System.out.println("PARAM_POLLING_INTERVAL"+getParameterInt(PARAM_POLLING_INTERVAL, true));
        return getParameterInt(PARAM_POLLING_INTERVAL, true);
    }

    public abstract void directoryScanPoll();

    public abstract void producerPoll();


    public abstract void consumerPoll();


    public void doStart()
    {
        producerTimer = new Timer(getName()+".ProducerTimer", false);

        //timer.scheduleAtFixedRate(new PollTask(), 0, getInterval() * 1000);
        producerTimer.scheduleAtFixedRate(new ProducerPollTask(), 0, 1000);

        consumerTimer = new Timer(getName()+".ConsumerTimer", false);

        //timer.scheduleAtFixedRate(new PollTask(), 0, getInterval() * 1000);
        consumerTimer.scheduleAtFixedRate(new ConsumerPollTask(), 0, 1000);


        timer = new Timer(getName()+".DirectoryScannerTimer", false);

        //timer.scheduleAtFixedRate(new PollTask(), 0, getInterval() * 1000);
        timer.scheduleAtFixedRate(new DirectoryScanPollTask(), 0, 1000);



    }

    public void doStop()
    {

        if (timer != null)
        {
            timer.cancel();
            timer = null;
        }

        if (producerTimer != null)
        {
            producerTimer.cancel();
            producerTimer = null;
        }

        if (consumerTimer != null)
        {
            consumerTimer.cancel();
            consumerTimer = null;
        }

    }

    private boolean isBusy()
    {
        return busy;
    }

    protected void setBusy(boolean b)
    {
        busy = b;
    }


    private boolean isProducerBusy()
    {
        return producerBusy;
    }

    protected void setProducerBusy(boolean b)
    {
        producerBusy = b;
    }



    private boolean isConsumerBusy()
    {
        return consumerBusy;
    }

    protected void setConsumerBusy(boolean b)
    {
        consumerBusy = b;
    }

    private class DirectoryScanPollTask extends TimerTask {
        public void run()
        {


            if (!isBusy())
            {
                setBusy(true);
                //System.out.println("polled called @" +DateTime.now().toString());
                directoryScanPoll();

                setBusy(false);


            } else
            {
                System.out.println("Miss DirectoryScanner tick");
            }


        }
    }

    private class ProducerPollTask extends TimerTask {
        public void run()
        {


            if (!isProducerBusy())
            {
                setProducerBusy(true);
                //System.out.println("polled called @" +DateTime.now().toString());
                producerPoll();

                setProducerBusy(false);


            } else
            {
                System.out.println("Miss Producer tick");
            }


        }
    }



    private class ConsumerPollTask extends TimerTask {
        public void run()
        {


            if (!isConsumerBusy())
            {
                setConsumerBusy(true);
                //System.out.println("polled called @" +DateTime.now().toString());
                consumerPoll();

                setConsumerBusy(false);


            } else
            {
                System.out.println("Miss Consumer tick");
            }


        }
    }



}
