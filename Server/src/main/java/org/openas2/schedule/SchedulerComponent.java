package org.openas2.schedule;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openas2.*;
import org.openas2.params.InvalidParameterException;

/**
 * Scheduler module for periodic tasks.
 */
public class SchedulerComponent extends BaseComponent {

    public static final String PARAMETER_THREADS = "threads";
    //
    private static final int MIN_AMOUNT_OF_THREADS = 6;
    private Log logger = LogFactory.getLog(SchedulerComponent.class.getSimpleName());

    private ScheduledExecutorService executorService;

    @Override
    public void init(Session session, Map<String, String> parameters) throws OpenAS2Exception
    {
        super.init(session, parameters);
        createExecutor();
        scheduleComponentsTasks(session);
    }

    private void createExecutor() throws InvalidParameterException
    {
        int configuredAmountOfThreads = getParameterInt(PARAMETER_THREADS, false);
        int amountOfThreads = configuredAmountOfThreads < MIN_AMOUNT_OF_THREADS ? MIN_AMOUNT_OF_THREADS : configuredAmountOfThreads;
        BasicThreadFactory threadFactory = new BasicThreadFactory.Builder()
                .namingPattern(getName() + "-Thread-%d")
                .build();

        this.executorService = Executors.newScheduledThreadPool(amountOfThreads, threadFactory);
        logger.debug("Scheduler module is ready.");
    }

    public void setReloadSession(Runnable runnable)
    {
        this.executorService.scheduleAtFixedRate(runnable,100, 100, TimeUnit.SECONDS);
    }
    private void scheduleComponentsTasks(Session session) throws OpenAS2Exception
    {
        for (Component component : session.getComponents().values())
        {
            if (HasSchedule.class.isAssignableFrom(component.getClass()))
            {
                HasSchedule.class.cast(component).schedule(executorService);
            }
        }
    }

    @Override
    public void destroy() throws Exception
    {
        //graceful shutdown
        executorService.awaitTermination(3, TimeUnit.SECONDS);
        executorService.shutdownNow();
    }
}