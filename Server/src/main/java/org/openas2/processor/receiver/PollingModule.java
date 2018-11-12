
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

                poll();
                setBusy(false);
               

            } else
            {
                System.out.println("Miss tick");
            }


        }
    }



}
