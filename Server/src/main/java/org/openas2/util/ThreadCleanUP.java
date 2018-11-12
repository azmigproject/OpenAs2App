package org.openas2.util;

public class ThreadCleanUP extends Thread

{

    // Constructor
    public ThreadCleanUP()
    {

    }

    // Main thread procedure
    public void run()
    {

        while(true)
        {
            try
            {
                sleep( 120000 );
            }
            catch( Exception e )
            {
            }

            System.runFinalization();
            System.gc();
            try
            {
                sleep( 1000 );
            }
            catch( Exception e )
            {
            }
            System.gc();
        }
    }
}
