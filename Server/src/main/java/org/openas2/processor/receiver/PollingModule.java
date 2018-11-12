
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

import org.openas2.OpenAS2Exception;
import org.openas2.Session;
import org.openas2.params.InvalidParameterException;
import org.openas2.util.ThreadCleanUP;


public abstract class PollingModule extends MessageBuilderModule {
    private static final String PARAM_POLLING_INTERVAL = "interval";
    private Timer timer;
    private boolean busy;


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

    public abstract void poll();


    public void doStart() throws OpenAS2Exception
    {

        timer = new Timer(getName(), false);

        //timer.scheduleAtFixedRate(new PollTask(), 0, getInterval() * 1000);
        timer.scheduleAtFixedRate(new PollTask(), 0, 1000);

    }

    public void doStop() throws OpenAS2Exception
    {

        if (timer != null)
        {
            timer.cancel();
            timer = null;
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

    private class PollTask extends TimerTask {
        public void run()
        {
            System.out.println("Busy"+busy);

            if (!isBusy())
            {
                setBusy(true);
                System.out.println("In busy If condition"+busy);
                poll();
                setBusy(false);
                System.out.println("In busy reset"+busy);
                System.out.println("start thread cleaning process"+busy);

                Thread th=new ThreadCleanUP();
                Runnable r3 = new Runnable() {
                    @Override
                    public void run() {
                       new ThreadCleanUP().run();
                    }
                };

                ExecutorService executor = Executors.newSingleThreadExecutor();
                executor.execute(r3);
                executor.shutdown();
                try {
                    executor.awaitTermination(1, TimeUnit.MINUTES);
                }
                catch (Exception exp)
                {

                }
                finally {
                    executor.shutdownNow();

                }
                System.out.println("Reset setBusy");

            } else
            {
                System.out.println("Miss tick");
            }


        }
    }



}
