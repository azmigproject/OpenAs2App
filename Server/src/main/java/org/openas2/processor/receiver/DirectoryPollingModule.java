package org.openas2.processor.receiver;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openas2.OpenAS2Exception;
import org.openas2.Session;
import org.openas2.XMLSession;
import org.openas2.message.Message;
import org.openas2.params.InvalidParameterException;
import org.openas2.util.DateUtil;
import org.openas2.util.IOUtilOld;
import org.openas2.util.QueueHelper;
import org.openas2.Constants;
import sun.util.calendar.BaseCalendar;

import javax.security.auth.callback.Callback;

import static java.lang.Thread.MAX_PRIORITY;
import static java.lang.Thread.NORM_PRIORITY;

public abstract class DirectoryPollingModule extends PollingModule
{
    public static final String PARAM_OUTBOX_DIRECTORY = "outboxdir";
    public static final String PARAM_FILE_EXTENSION_FILTER = "fileextensionfilter";
    private ConcurrentHashMap<String, Long> trackedFiles;
    private ConcurrentHashMap<String, Long> trackAddedToPoolFiles;
    private String outboxDir;
    private String errorDir;
    private String sentDir = null;
    private  int MAX_FileThread = 15;
    private int MAX_QueueThread=15;
    private int MAX_DirectoryThread=1;
    private int FileThreadCounter=0;
    private int QueueThreadCounter=0;
    private int DirWatcherTheadCounter=0;
    private QueueHelper queueHelper;
    BlockingQueue FileBlockingQueue;
    BlockingQueue FileProcessingBlockingQueue;
    ExecutorService PollPool;
    private Log logger = LogFactory.getLog(DirectoryPollingModule.class.getSimpleName());

    public void init(Session session, Map<String, String> options) throws OpenAS2Exception
    {
        super.init(session, options);
        // Check all the directories are configured and actually exist on the file system
        try
        {

            MAX_QueueThread=session.getMaxQueueDownloaderThread();
            MAX_FileThread=session.getMaxFileProcessorThread();
            MAX_DirectoryThread=session.getMaxDirWatcherThread();
            FileBlockingQueue=new ArrayBlockingQueue(1000);
            FileProcessingBlockingQueue=new ArrayBlockingQueue(1000);
            outboxDir = getParameter(PARAM_OUTBOX_DIRECTORY, true);
            IOUtilOld.getDirectoryFile(outboxDir);
            errorDir = getParameter(PARAM_ERROR_DIRECTORY, true);
            IOUtilOld.getDirectoryFile(errorDir);
            sentDir = getParameter(PARAM_SENT_DIRECTORY, false);
            if (sentDir != null)
                IOUtilOld.getDirectoryFile(sentDir);
            String pendingInfoFolder = getSession().getProcessor().getParameters().get("pendingmdninfo");
            IOUtilOld.getDirectoryFile(pendingInfoFolder);
            String pendingFolder = getSession().getProcessor().getParameters().get("pendingmdn");
            IOUtilOld.getDirectoryFile(pendingFolder);

        } catch (IOException e)
        {
            throw new OpenAS2Exception("Failed to initialise directory poller.", e);
        }
    }

    @Override
    public boolean healthcheck(List<String> failures)
    {
        try
        {
            IOUtilOld.getDirectoryFile(outboxDir);
        } catch (IOException e)
        {
            failures.add(this.getClass().getSimpleName() + " - Polling directory is not accessible: " + outboxDir);
            return false;
        }
        return true;
    }

    public void poll()
    {
       // while (true) {
            try {

                final int noOfFilesAllowedToDownload = 32;
                boolean isMsgInQueue = false;
                queueHelper = new QueueHelper();
                //System.out.println( "Polling started at" +outboxDir);
                isMsgInQueue = queueHelper.GetMsgFromQueue(outboxDir, 1,FileBlockingQueue);


                if (isMsgInQueue || !FileBlockingQueue.isEmpty() || getFilesBasedOnFilter(IOUtilOld.getDirectoryFile(outboxDir), "downloaded").length > 0)

                {

                   // System.out.println( "Threading condition validate");


                    Runnable   producer = new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    boolean isMsgInQueue = true;
                                    while (isMsgInQueue && FileBlockingQueue.remainingCapacity()>0) {

                                        isMsgInQueue = queueHelper.GetMsgFromQueue(outboxDir, noOfFilesAllowedToDownload,FileBlockingQueue);
                                        synchronized (this) {
                                            try {
                                                Thread.currentThread().wait(100);
                                            }
                                            catch (InterruptedException e) {
                                               // e.printStackTrace();

                                            }
                                        }

                                    }


                                } catch (Exception ex) {

                                    StringWriter sw = new StringWriter();
                                    ex.printStackTrace(new PrintWriter(sw));
                                    System.out.println(" Error in downloading file from queue" + (new Date()).toString() + "Exception" + ex.getMessage() + sw.toString());
                                    logger.error("Error in downloading file from queue : " + outboxDir, ex);
                                }
                                finally
                                {
                                    QueueThreadCounter--;
                                }

                            }
                        };


                    Runnable directoryWatcher=new Runnable() {
                        @Override
                        public void run() {
                          try
                          {

                              if((FileBlockingQueue.isEmpty() ||FileBlockingQueue.size()==0)) {
                                  File[] Files = getFilesBasedOnFilter(IOUtilOld.getDirectoryFile(outboxDir), "downloaded");
                                  int dirFileLength = Files != null ? Files.length : 0;
                                  if (dirFileLength > 0) {


                                      if (dirFileLength > 0) {
                                          int dirFileCounter = 0;
                                          while (dirFileLength > 0) {
                                              scanDirectory(Files);
                                              synchronized (this) {
                                                  try {
                                                      Thread.currentThread().wait(100);
                                                  } catch (InterruptedException e) {
                                                     // e.printStackTrace();
                                                  }
                                              }

                                              System.out.println(" Scan Directry  for" + dirFileLength + "  Files" + "in dir" + outboxDir);
                                              logger.info(" Scan Directry  for" + dirFileLength + "  Files" + "in dir" + outboxDir);
                                              dirFileCounter++;
                                              if (dirFileCounter == dirFileLength) {
                                                  Files = getFilesBasedOnFilter(IOUtilOld.getDirectoryFile(outboxDir), "downloaded");
                                                  dirFileLength = Files != null ? Files.length : 0;
                                                  dirFileCounter = 0;
                                              }
                                          }
                                      }
                                  }
                              }
                          }
                          catch (Exception ex)
                          {
                              StringWriter sw = new StringWriter();
                              ex.printStackTrace(new PrintWriter(sw));
                              System.out.println(" Error in downloading file from queue" + (new Date()).toString() + "Exception" + ex.getMessage() + sw.toString());
                              logger.error("Error in downloading file from queue : " + outboxDir, ex);
                          }
                          finally
                          {
                              FileThreadCounter--;
                          }
                        }
                    };


                    Runnable consumer = new Runnable() {
                        @Override
                        public void run() {
                            try {

                                while (FileBlockingQueue.size() > 0) {
                                    updateTracking();

                                    System.out.println("BlockingQueue Length" + FileBlockingQueue.size());
                                    logger.info("BlockingQueue Length" + FileBlockingQueue.size());
                                    if (FileBlockingQueue.size() == 0) {
                                        synchronized (this) {
                                            try {
                                                Thread.currentThread().wait(100);
                                            }
                                            catch (InterruptedException e) {
                                               // e.printStackTrace();

                                            }
                                        }

                                    }
                                }


                            } catch (Exception ex) {
                                StringWriter sw = new StringWriter();
                                ex.printStackTrace(new PrintWriter(sw));
                                System.out.println(" Error in Scan Directry at" + (new Date()).toString() + "Exception" + ex.getMessage() + " " + sw.toString());
                                logger.error("Error in Scan Directry : " + outboxDir, ex);
//
                            }
                            finally
                            {
                                DirWatcherTheadCounter--;
                            }
                        }
                    };

                    while(QueueThreadCounter <MAX_QueueThread)

                    {
                        Thread producerThread = new Thread(producer, "ProducerThread");
                        producerThread.setPriority(NORM_PRIORITY+2); //QueueDownloader Max Thread
                        producerThread.start();
                        QueueThreadCounter++;
                    }

                    while(FileThreadCounter <MAX_FileThread)

                    {
                        Thread consumerThread = new Thread(consumer, "ConsumerThread");
                        consumerThread.setPriority(NORM_PRIORITY+1); //FileProcessor Max Thread
                        consumerThread.start();
                        FileThreadCounter++;
                    }
                    // Add conter values at too
                    // add finally in each runable deff and reduce the counter value
                    // instead of for conver it to while loop based on te couter value & max thread condition
                    while(DirWatcherTheadCounter <MAX_DirectoryThread)

                    {
                        Thread dirWatcherThread = new Thread(directoryWatcher, "DirWatcherThread");
                        dirWatcherThread.setPriority(NORM_PRIORITY);  //DirWatcher Max Thread
                        dirWatcherThread.start();
                        DirWatcherTheadCounter++;
                    }
                    System.out.println("PollPool Executer Terminated at" + (new Date()).toString());
                    logger.info("PollPool Executer Terminated at" + (new Date()).toString());
                }
                else
                {
                    //System.out.println( "Threading condition invalidate in this poll");
                }


            } catch (Exception e) {
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                System.out.println("Directry polled at" + (new Date()).toString() + "Exception" + e.getMessage() + "" + sw.toString());
                System.out.println(e.getStackTrace().toString());
                logger.error("Unexpected error occurred polling directory for files to send: " + outboxDir, e);
            }

       // }

    }


    protected void scanDirectory(File[] files) throws IOException, InvalidParameterException
    {

        try {


            // iterator through each entry, and start tracking new files
            if (files!=null && files.length > 0) {

                for (int i = 0; i < files.length; i++) {
                    File currentFile = files[i];

                    if (checkFile(currentFile)) {
                        // start watching the file's size if it's not already being
                        // watched
                        trackFile(currentFile);
                    }
                }
            }


        }
        catch (Exception exp) {
            StringWriter sw = new StringWriter();
            exp.printStackTrace(new PrintWriter(sw));
            System.out.println("Error occured in scanDirectory " + exp.getMessage()+sw.toString());
        }
    }




    private File[] getFilesBasedOnFilter(File dir, final String extensionFilter)
    {
        try {
            return dir.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    if(!name.toLowerCase().contains("error")) {
                        return name.toLowerCase().endsWith("." + extensionFilter);
                    }
                    else

                    {
                        return false;
                    }
                }
            });
        }
        catch (Exception exp)
        {
            System.out.println("Error occured in getFilesBasedOnFilter for dir="+dir+"; for ext.filter="+extensionFilter+"; Error="+ exp.getMessage());
            return null;
        }
    }


    protected boolean checkFile(File file)
    {
        if (file.exists() && file.isFile())
        {
            try
            {
                // check for a write-lock on file, will skip file if it's write
                // locked
                FileOutputStream fOut = new FileOutputStream(file, true);
                fOut.close();
                return true;
            } catch (IOException ioe)
            {
                // a sharing violation occurred, ignore the file for now
                if (logger.isDebugEnabled())
                {
                    try
                    {
                        logger.debug("Directory poller detected a non-writable file and will be ignored: " + file.getCanonicalPath());
                    } catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                }
            }
        }
        return false;
    }

    private void trackFile(File file) {

        synchronized (FileBlockingQueue) {
            synchronized (FileProcessingBlockingQueue) {
                String filePath = file.getAbsolutePath();

                if (FileBlockingQueue.remainingCapacity() > 0 && !FileBlockingQueue.contains(filePath) && !FileProcessingBlockingQueue.contains(filePath)) {
                    FileBlockingQueue.add(filePath);
                    System.out.println("Track file and add it in  Tracked file list"+filePath );
                    logger.info("Track file and add it in  Tracked file list"+filePath);
                }
            }
        }
    }

    private String GetFileFromQueue() {
        String strFileName = "";
        synchronized (FileBlockingQueue) {
            synchronized (FileProcessingBlockingQueue) {

                if (FileBlockingQueue.size() > 0) {
                    Object obj = null;
                    try {
                        obj = FileBlockingQueue.poll(1, TimeUnit.SECONDS);
                    } catch (InterruptedException exp) {
                    }
                    if (obj != null) {
                        strFileName = (String) obj;
                        FileProcessingBlockingQueue.add(strFileName);
                        System.out.println("Get file from queue for procesing"+strFileName);
                        System.out.println("Add file in FileProcessingBlockingQueue "+strFileName);
                        logger.info("Get file from queue for procesing"+strFileName);
                        logger.info("Add file in FileProcessingBlockingQueue "+strFileName);
                    }
                }
            }
            return strFileName;
        }
    }


    private void updateTracking() {
        // clone the trackedFiles map, iterator through the clone and modify the
        // original to avoid iterator exceptions
        // is there a better way to do this?
        //System.out.println("Max Thread in updateTracking " + getSession().getMaxDirectoryPoolingThread());

        try {
            String strFile ="";
            strFile = GetFileFromQueue();
            if (strFile!="") {
                System.out.println("start processing for"+strFile);
                UpdateTrackingTask(strFile);

                synchronized (FileProcessingBlockingQueue) {
                    System.out.println("Remove from FileProcessingBlockingQueue" + strFile);
                    logger.info("Remove from FileProcessingBlockingQueue" + strFile);
                    FileProcessingBlockingQueue.remove(strFile);
                }

            }

        } catch (Exception exp) {

            System.out.println("Error occured in updateTracking" + exp.getMessage());
            logger.info("Error occured in updateTracking" + exp.getMessage());
        }

    }




    public void UpdateTrackingTask(String fileEntry )
    {


        //System.out.println("In UpdateTrackingTask"+fileEntry.getKey());
        // get the file and it's stored length
        File file = new File(fileEntry);

        try {
            // if the file no longer exists, remove it from the tracker
            if (!checkFile(file)) {


            } else {
                // if the file length has changed, update the tracker

                // if the file length has stayed the same, process the file
                // and stop tracking it
                try {
                    File newFile = new File(fileEntry.replace(".downloaded", ".processing"));
                    file.renameTo(newFile);
                    file = newFile;
                    System.out.println("File Renamed" + file.toString());
                    logger.info("File Renamed" + file.toString());
                    processFile(file);
                } catch (OpenAS2Exception e) {
                    System.out.println("Error occured in UpdateTrackingTask" + e.getMessage());
                    logger.info("Error occured in UpdateTrackingTask" + e.getMessage());
                    e.terminate();
                    try {
                        //IOUtilOld.handleError(file, errorDir);
                        IOUtilOld.handleError(file, errorDir);
                    } catch (OpenAS2Exception e1) {
                        logger.error("Error handling file error for file: " + file.getAbsolutePath(), e1);
                        System.out.println("Error occured in UpdateTrackingTask" + e.getMessage());
                       //forceStop(e1);
                        return;
                    }
                }
            }
        }
        catch (Exception ex)
        {
            System.out.println("Error occured in UpdateTrackingTask" + ex.getMessage());
        }

    }




    protected void processFile(File file) throws OpenAS2Exception
    {

        if (logger.isInfoEnabled())
            logger.info("processing " + file.getAbsolutePath());

        try
        {
            System.out.println("Document processing starts " + file.getAbsolutePath());
            logger.info("Document processing starts " + file.getAbsolutePath());
            processDocument(new FileInputStream(file), file.getName().replace(".processing","").trim());
            System.out.println("Document processing completed " + file.getAbsolutePath());
            logger.info("Document processing completed " + file.getAbsolutePath());
            try
            {
                IOUtilOld.deleteFile(file);
                System.out.println("Document processing deleted " + file.getAbsolutePath());
                logger.info("Document processing deleted " + file.getAbsolutePath());
            } catch (IOException e)
            {
                throw new OpenAS2Exception("Failed to delete file handed off for processing:" + file.getAbsolutePath(), e);
            }
        } catch (FileNotFoundException e)
        {
            throw new OpenAS2Exception("Failed to process file:" + file.getAbsolutePath(), e);
        }
    }


    protected abstract Message createMessage();


}