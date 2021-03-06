package org.openas2.processor.receiver;

import java.io.*;
import java.nio.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.openas2.OpenAS2Exception;
import org.openas2.Session;
import org.openas2.XMLSession;
import org.openas2.message.Message;
import org.openas2.message.SimpleLogMessage;
import org.openas2.params.InvalidParameterException;
import org.openas2.util.DateUtil;
import org.openas2.util.HighPerformanceBlockingQueue;
import org.openas2.util.IOUtilOld;
import org.openas2.util.QueueHelper;
import org.openas2.Constants;
import sun.util.calendar.BaseCalendar;

import javax.security.auth.callback.Callback;

import static java.lang.Thread.MAX_PRIORITY;
import static java.lang.Thread.NORM_PRIORITY;

public abstract class DirectoryPollingModule extends PollingModule {
    public static final String PARAM_OUTBOX_DIRECTORY = "outboxdir";
    public static final String PARAM_FILE_EXTENSION_FILTER = "fileextensionfilter";
    private String outboxDir;
    private String errorDir;
    private String sentDir = null;
    private int MAX_FileThread = 15;
    private int MAX_QueueThread = 15;
    private int MAX_DirectoryThread = 1;
    private int FileThreadCounter = 0;
    private int QueueThreadCounter = 0;
    private int DirWatcherThreadCounter = 0;
    private int ActiveQueueThreadCounter = 0;
    private int ActiveFileThreadCounter = 0;
    private int ActiveDirWatcherThreadCounter = 0;
    //BlockingQueue FileBlockingQueue;
    BlockingQueue<String> FileProcessingBlockingQueue;
    private int BlockingQueueSizeSize = 1000;
    private int FileWatcherStalenessThresholdInSeconds = 90;

    private ThreadGroup consumerThreadGroup = null;
    private ThreadGroup producerThreadGroup  = null;
    private ThreadGroup dirWatcherThreadGroup = null;

    ConcurrentMap<String, String> RunningQueueThreads;
    HighPerformanceBlockingQueue FileBlockingQueue;

    private Log logger = LogFactory.getLog(DirectoryPollingModule.class.getSimpleName());

    public void init(Session session, Map<String, String> options) throws OpenAS2Exception {
        super.init(session, options);
        // Check all the directories are configured and actually exist on the file system
        try {

            MAX_QueueThread = session.getMaxQueueDownloaderThread();
            MAX_FileThread = session.getMaxFileProcessorThread();
            MAX_DirectoryThread = session.getMaxDirWatcherThread();
            BlockingQueueSizeSize = session.getBlockingQueueSizeSize();
            FileWatcherStalenessThresholdInSeconds = session.getFileWatcherStalenessThresholdInSeconds();
            RunningQueueThreads = new ConcurrentHashMap<String, String>();
            FileProcessingBlockingQueue = new LinkedBlockingQueue<String>();
            FileBlockingQueue = new HighPerformanceBlockingQueue(BlockingQueueSizeSize);
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

        } catch (IOException e) {
            throw new OpenAS2Exception("Failed to initialise directory poller.", e);
        }
    }

    @Override
    public boolean healthcheck(List<String> failures) {
        try {
            IOUtilOld.getDirectoryFile(outboxDir);
        } catch (IOException e) {
            failures.add(this.getClass().getSimpleName() + " - Polling directory is not accessible: " + outboxDir);
            return false;
        }
        return true;
    }

    public void directoryScanPoll() {
        // while (true) {
        try {



            if (dirWatcherThreadGroup == null)
                dirWatcherThreadGroup = new ThreadGroup("DirWatcher-"+outboxDir);

            ActiveDirWatcherThreadCounter = dirWatcherThreadGroup.activeCount();


            if (FileBlockingQueue.size() == 0 && getFilesBasedOnFilter(IOUtilOld.getDirectoryFile(outboxDir), "downloaded").length > 0)

            {


                Runnable directoryWatcher = new Runnable() {
                    @Override
                    public void run() {
                        try {

                            if (FileBlockingQueue.size() == 0) {
                                File[] Files = getFilesBasedOnFilterWithAge(IOUtilOld.getDirectoryFile(outboxDir), "downloaded",FileWatcherStalenessThresholdInSeconds);
                                int dirFileLength = Files != null ? Files.length : 0;


                                if (dirFileLength > 0) {
                                    int dirFileCounter = 0;
                                    while (dirFileLength > 0) {
                                        if (FileBlockingQueue.size() == 0) {
                                            scanDirectory(Files);
                                            synchronized (this) {
                                                try {
                                                    //Thread.currentThread().wait(100);
                                                    Thread.sleep(500);
                                                } catch (InterruptedException e) {
                                                    // e.printStackTrace();
                                                }
                                            }
                                        }

                                        System.out.println(" Scan Directry  for" + dirFileLength + "  Files" + "in dir" + outboxDir);
                                        if(logger.isDebugEnabled())
                                            logger.debug(" Scan Directry  for" + dirFileLength + "  Files" + "in dir" + outboxDir);
                                        dirFileCounter++;
                                        //if (dirFileCounter == dirFileLength) {

                                        Files = getFilesBasedOnFilterWithAge(IOUtilOld.getDirectoryFile(outboxDir), "downloaded", FileWatcherStalenessThresholdInSeconds);
                                        dirFileLength = Files != null ? Files.length : 0;
                                        // dirFileCounter = 0;


                                    }
                                }

                            } else {

                            }
                        } catch (Exception ex) {
                            StringWriter sw = new StringWriter();
                            ex.printStackTrace(new PrintWriter(sw));
                            System.out.println(" Error in directoryWatcher in directoryScanPoll " + (new Date()).toString() + "Exception" + ex.getMessage() + sw.toString());
                            logger.error("Error in directoryWatcher in directoryScanPoll : " + outboxDir, ex);
                        } finally {
                            if(DirWatcherThreadCounter >=1)
                                DirWatcherThreadCounter--;
                        }
                    }
                };




                // Add conter values at too
                // add finally in each runable deff and reduce the counter value
                // instead of for conver it to while loop based on te couter value & max thread condition
                //while (DirWatcherThreadCounter < MAX_DirectoryThread)
                while (ActiveDirWatcherThreadCounter < MAX_DirectoryThread)
                {
                    //Thread dirWatcherThread = new Thread(directoryWatcher, "DirWatcherThread" + DirWatcherThreadCounter);
                    Thread dirWatcherThread = new Thread(dirWatcherThreadGroup, directoryWatcher, "DirWatcherThread" + DirWatcherThreadCounter);
                    dirWatcherThread.setPriority(NORM_PRIORITY);  //DirWatcher Max Thread
                    dirWatcherThread.start();
                    DirWatcherThreadCounter++;
                    ActiveDirWatcherThreadCounter = dirWatcherThreadGroup.activeCount();
                    if(logger.isDebugEnabled())
                    logger.debug("**** ActiveDirWatcherThreadCounter" + ActiveDirWatcherThreadCounter +" QueueThreadCounter "+DirWatcherThreadCounter+ " :"+outboxDir);
                }
                System.out.println("Directory Scan PollPool Executer Terminated at" + (new Date()).toString());
                if(logger.isDebugEnabled())
                    logger.debug("Directory Scan PollPool Executer Terminated at" + (new Date()).toString());
            } else {
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

    public void producerPoll()
    {
        try {

            if (producerThreadGroup == null)
                producerThreadGroup = new ThreadGroup("Producer-"+outboxDir);

            ActiveQueueThreadCounter = producerThreadGroup.activeCount();

            final int noOfFilesAllowedToDownload = 1;//32;
            boolean isMsgInQueue = false;
            QueueHelper queueHelper=new QueueHelper();
            //System.out.println( "Polling started at" +outboxDir);
            isMsgInQueue = queueHelper.GetMsgFromQueue(outboxDir, 1, FileBlockingQueue);


            if (isMsgInQueue)

            {

                // System.out.println( "Threading condition validate");


                Runnable producer = new Runnable() {
                    @Override
                    public void run() {
                        try {
                            boolean isMsgInQueue = true;
                            QueueHelper prodQueueHelper = new QueueHelper();
                            while (isMsgInQueue) {

                                isMsgInQueue = prodQueueHelper.GetMsgFromQueue(outboxDir, noOfFilesAllowedToDownload, FileBlockingQueue);
                                synchronized (this) {
                                    try {
                                        // Thread.currentThread().wait(100);
                                        Thread.sleep(500);
                                    } catch (InterruptedException e) {
                                        // e.printStackTrace();

                                    }
                                }

                            }


                        } catch (Exception ex) {

                            StringWriter sw = new StringWriter();
                            ex.printStackTrace(new PrintWriter(sw));
                            System.out.println(" Error in Producer thread for directory polling module " + (new Date()).toString() + "Exception" + ex.getMessage() + sw.toString());
                            logger.error("Error in Producer thread for directory polling module  : " + outboxDir, ex);
                        } finally {
                            if(QueueThreadCounter >=1)
                                QueueThreadCounter--;
                        }

                    }
                };



                //while (QueueThreadCounter < MAX_QueueThread)
                while (ActiveQueueThreadCounter < MAX_QueueThread)
                {
                    //Thread producerThread = new Thread(producer, "ProducerThread" + QueueThreadCounter);
                    Thread producerThread = new Thread(producerThreadGroup, producer, "ProducerThread" + QueueThreadCounter);
                    producerThread.setPriority(NORM_PRIORITY + 2); //QueueDownloader Max Thread
                    producerThread.start();
                    QueueThreadCounter++;
                    ActiveQueueThreadCounter = producerThreadGroup.activeCount();
                    if(logger.isDebugEnabled())
                    logger.debug("**** ActiveProducerThreadCounter" + ActiveQueueThreadCounter +" QueueThreadCounter "+QueueThreadCounter + " :"+outboxDir);
                }


                System.out.println("Producer PollPool Executer Terminated at" + (new Date()).toString());
                if(logger.isDebugEnabled())
                    logger.debug("Producer PollPool Executer Terminated at" + (new Date()).toString());
            } else {
                //System.out.println( "Threading condition invalidate in this poll");
            }


        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            System.out.println("Producer polled at" + (new Date()).toString() + "Exception" + e.getMessage() + "" + sw.toString());
            System.out.println(e.getStackTrace().toString());
            logger.error("Unexpected error occurred polling Producer Polling for files to send: " + outboxDir, e);
        }
    }


    public void consumerPoll()
    {
        try {


            if (consumerThreadGroup == null)
                consumerThreadGroup = new ThreadGroup("Consumer-"+outboxDir);

            ActiveFileThreadCounter = consumerThreadGroup.activeCount();

            if (FileBlockingQueue.size() > 0 )

            {



                //while (FileThreadCounter < MAX_FileThread)
                while (FileBlockingQueue.size() > 0 && (ActiveFileThreadCounter == FileBlockingQueue.size() || ActiveFileThreadCounter < MAX_FileThread))
                {
                    //Thread consumerThread = new Thread(GetConsumer("ConsumerThread" + FileThreadCounter), "ConsumerThread" + FileThreadCounter);
                    Thread consumerThread = new Thread(consumerThreadGroup, GetConsumer("ConsumerThread" + FileThreadCounter), "ConsumerThread" + FileThreadCounter);
                    consumerThread.setPriority(NORM_PRIORITY + 1); //FileProcessor Max Thread
                    RunningQueueThreads.putIfAbsent("ConsumerThread" + FileThreadCounter, "");
                    consumerThread.start();
                    FileThreadCounter++;
                    ActiveFileThreadCounter = consumerThreadGroup.activeCount();
                    if(logger.isDebugEnabled())
                    logger.debug("**** ActiveConsumerThreadCounter" + ActiveFileThreadCounter +" FileThreadCounter "+FileThreadCounter + " :"+outboxDir);
                }


                System.out.println("Consumer PollPool Executer Terminated at" + (new Date()).toString());
                if(logger.isDebugEnabled())
                    logger.debug("Consumer PollPool Executer Terminated at" + (new Date()).toString());
            } else {
                //System.out.println( "Threading condition invalidate in this poll");
            }


        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            System.out.println("Consumer polled at" + (new Date()).toString() + "Exception" + e.getMessage() + "" + sw.toString());
            System.out.println(e.getStackTrace().toString());
            logger.error("Unexpected error occurred polling Consumer for files to send: " + outboxDir, e);
        }
    }




    protected void scanDirectory(File[] files) throws IOException, InvalidParameterException {

        try {


            // iterator through each entry, and start tracking new files
            if (files != null && files.length > 0) {

                for (int i = 0; i < files.length; i++) {
                    File currentFile = files[i];

                    if (checkFileAndTrack(currentFile)) {
                        // start watching the file's size if it's not already being
                        // watched
                        //trackFile(currentFile);
                    } else {
                        System.out.println("File not accessible and not tracked  " + currentFile);
                    }
                }
            }


        } catch (Exception exp) {
            StringWriter sw = new StringWriter();
            exp.printStackTrace(new PrintWriter(sw));
            System.out.println("Error occured in scanDirectory " + exp.getMessage() + sw.toString());
        }
    }


    private Runnable GetConsumer(final String ThreadName) {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    while (FileBlockingQueue.size() > 0) {
                        updateTracking(ThreadName);

                        System.out.println("BlockingQueue Length" + FileBlockingQueue.size());
                        if(logger.isDebugEnabled())
                            logger.debug("BlockingQueue Length" + FileBlockingQueue.size());
                        if (FileBlockingQueue.size() > 0) {
                            synchronized (this) {
                                try {
                                    //  Thread.currentThread().wait(100);
                                    Thread.sleep(500);
                                } catch (InterruptedException e) {
                                    // e.printStackTrace();

                                }
                            }

                        }
                    }
                } catch (Exception ex) {
                    StringWriter sw = new StringWriter();
                    ex.printStackTrace(new PrintWriter(sw));
                    System.out.println(" Error in Consumer thread to process file at" + (new Date()).toString() + "Exception" + ex.getMessage() + " " + sw.toString());
                    logger.error("Error in Consumer thread to process file  : " + outboxDir, ex);
//
                } finally {

                    if (FileThreadCounter >=1)
                        --FileThreadCounter;

                    RunningQueueThreads.remove(ThreadName);

                    System.out.println("Finally executed in conumser thread now the FileThreadCounter value is " + FileThreadCounter);
                    if(logger.isDebugEnabled())
                        logger.debug("Finally executed in conumser thread now the RunningQueueThreads value is " + RunningQueueThreads.size());
                }
            }
        };
    }


    private File[] getFilesBasedOnFilter(File dir, final String extensionFilter) {
        try {
            return dir.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    if (!name.toLowerCase().contains("error")) {
                        return name.toLowerCase().endsWith("." + extensionFilter) ;
                    } else

                    {
                        return false;
                    }
                }
            });
        } catch (Exception exp) {
            System.out.println("Error occured in getFilesBasedOnFilter for dir=" + dir + "; for ext.filter=" + extensionFilter + "; Error=" + exp.getMessage());
            return null;
        }
    }

    private File[] getFilesBasedOnFilterWithAge(File dir, final String extensionFilter,final long staleIntervalInSeconds) {
        try {
            return dir.listFiles(new FileFilter() {
                public boolean accept(File f) {
                    if (f.isFile() && !new Date().before(new Date(f.lastModified()+(staleIntervalInSeconds*1000)))) {
                        return f.getName().toLowerCase().endsWith("." + extensionFilter);
                    } else {
                        return false;
                    }
                }
            });
        } catch (Exception exp) {
            System.out.println("Error occured in getFilesBasedOnFilter for dir=" + dir + "; for ext.filter=" + extensionFilter + "; Error=" + exp.getMessage());
            return null;
        }
    }


    protected boolean checkFile(File file) {
        if (file.exists() && file.isFile())

        {
            String filePath = file.getAbsolutePath();
            if (!FileBlockingQueue.contains(filePath)) {
                synchronized (FileBlockingQueue) {
                    synchronized (FileProcessingBlockingQueue) {
                        synchronized (RunningQueueThreads) {


                            if (!FileProcessingBlockingQueue.contains(filePath) && !RunningQueueThreads.containsValue(filePath)) {

                                {
                                    try {
                                        // check for a write-lock on file, will skip file if it's write
                                        // locked
                                        FileOutputStream fOut = new FileOutputStream(file, true);
                                        fOut.close();

                                        return true;
                                    } catch (IOException ioe) {
                                        // a sharing violation occurred, ignore the file for now
                                        if (logger.isDebugEnabled()) {
                                            try {
                                                logger.debug("Directory poller detected a non-writable file and will be ignored: " + file.getCanonicalPath());
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return false;
    }


    protected boolean checkFileAndTrack(File file) {
        if (file.exists() && file.isFile())

        {
            String filePath = file.getAbsolutePath();
            if (!FileBlockingQueue.contains(filePath)) {
                synchronized (FileBlockingQueue) {
                    synchronized (FileProcessingBlockingQueue) {
                        synchronized (RunningQueueThreads) {


                            if (!FileProcessingBlockingQueue.contains(filePath) && !RunningQueueThreads.containsValue(filePath)) {

                                {
                                    try {
                                        // check for a write-lock on file, will skip file if it's write
                                        // locked
                                        FileOutputStream fOut = new FileOutputStream(file, true);
                                        fOut.close();
                                        SimpleLogMessage sm=new SimpleLogMessage();

                                        sm.setFileName(file.getName());
                                        sm.setReceiverId(file.getParentFile().getName());
                                        if (!FileBlockingQueue.contains(filePath)) {
                                            if (!FileProcessingBlockingQueue.contains(filePath) && !RunningQueueThreads.containsValue(filePath)) {

                                                FileBlockingQueue.AddPath(filePath);
                                                System.out.println("Track file and add it in  Tracked file list (checkFileAndTrack)" + filePath);
                                                sm.setLogMessage("Track file and add it in  Tracked file list  (checkFileAndTrack)" + filePath);
                                                logger.info(sm);
                                                if(logger.isDebugEnabled())
                                                    logger.debug("FileTracked" + FileBlockingQueue.size() + "& file in processing  (checkFileAndTrack)" + FileProcessingBlockingQueue.size()+ " "+filePath);


                                            } else {
                                                System.out.println("Track file and not add it in  Tracked file list (checkFileAndTrack)" + filePath);
                                                sm.setLogMessage("Track file and not add it in  Tracked file list (checkFileAndTrack)" + filePath);
                                                logger.info(sm);
                                                if(logger.isDebugEnabled())
                                                    logger.debug(" (checkFileAndTrack) FileTracked" + FileBlockingQueue.size() + "& file in processing " + FileProcessingBlockingQueue.size());

                                            }
                                        }

                                        return true;
                                    } catch (IOException ioe) {
                                        // a sharing violation occurred, ignore the file for now
                                        if (logger.isDebugEnabled()) {
                                            try {
                                                logger.debug("Directory poller detected a non-writable file and will be ignored: " + file.getCanonicalPath());
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return false;
    }


//    private void trackFile(File file) {
//        String filePath = file.getAbsolutePath();
//        if (!FileBlockingQueue.contains(filePath)) {
//            synchronized (FileBlockingQueue) {
//                synchronized (FileProcessingBlockingQueue) {
//                    synchronized (RunningQueueThreads) {
//
//
//                        if (!FileProcessingBlockingQueue.contains(filePath) && !RunningQueueThreads.containsValue(filePath)) {
//
//                            FileBlockingQueue.AddPath(filePath);
//                            System.out.println("Track file and add it in  Tracked file list (trackFile)"+ filePath);
//                            logger.info("Track file and add it in  Tracked file list (trackFile)" + filePath);
//                            logger.info("FileTracked" + FileBlockingQueue.size() + "& file in processing (trackFile)" + FileProcessingBlockingQueue.size()+" "+filePath);
//
//
//                        } else {
//                            System.out.println("Track file and not add it in  Tracked file list" + filePath);
//                            logger.info("Track file and not add it in  Tracked file list" + filePath);
//                            logger.info("FileTracked" + FileBlockingQueue.size() + "& file in processing " + FileProcessingBlockingQueue.size());
//
//                        }
//                    }
//                }
//            }
//        }
//    }

    private String GetFileFromQueue(String ThreadName) {
        String strFileName = "";
        synchronized (FileBlockingQueue) {
            synchronized (FileProcessingBlockingQueue) {
                synchronized (RunningQueueThreads) {

                    if (FileBlockingQueue.size() > 0) {

                        try {
                            strFileName = FileBlockingQueue.GetPath();
                        } catch (InterruptedException exp) {
                            //System.out.println("Error ocur in file fetching from queue" + exp.getMessage());
                        } catch (Exception exp) {
                            System.out.println("Error ocur in file fetching from queue" + exp.getMessage());
                        }
                        if (strFileName != "") {

                            if (!RunningQueueThreads.containsValue(strFileName)) {
                                if (!FileProcessingBlockingQueue.contains(strFileName)) {
                                    FileProcessingBlockingQueue.add(strFileName);
                                    RunningQueueThreads.replace(ThreadName, strFileName);
                                    File file = new File(strFileName);
                                    SimpleLogMessage sm=new SimpleLogMessage();

                                    sm.setFileName(file.getName());
                                    sm.setReceiverId(file.getParentFile().getParentFile().getName());
                                    // System.out.println("Get file from queue for procesing" + strFileName);
                                    //System.out.println("Add file in FileProcessingBlockingQueue " + strFileName);
                                    // System.out.println("Thread " + ThreadName + "Process file " + strFileName);
                                    sm.setLogMessage("Get file from queue for procesing" + strFileName);
                                    logger.info(sm);
                                    sm.setLogMessage("Add file in FileProcessingBlockingQueue " + strFileName);
                                    logger.info(sm);
                                    if(logger.isDebugEnabled())
                                        logger.debug("Thread " + ThreadName + "Process file " + strFileName);

                                    File newFile = new File(strFileName.replace(".downloaded", ".processing"));
                                    file.renameTo(newFile);
                                    //file = newFile;
                                    System.out.println("File Renamed" + file.toString());
                                    sm.setFileName(newFile.getName());
                                    sm.setLogMessage("File " + file.toString() +"renamed to "+newFile.getName());
                                    logger.info(sm);
                                    strFileName = strFileName.replace(".downloaded", ".processing");
                                } else

                                {
                                    System.out.println("File Already in processing so not get the file " + strFileName);
                                    if(logger.isDebugEnabled())
                                        logger.debug("File Already in processing so not get the file " + strFileName);
                                    strFileName = "";
                                }
                            } else {
                                System.out.println("File Already in processing so not get the file " + strFileName);
                                if(logger.isDebugEnabled()) logger.debug("File Already in processing so not get the file " + strFileName);
                                strFileName = "";
                            }

                        } else {
                            System.out.println("No file get from queue and queuelength" + FileBlockingQueue.size());
                        }
                    }
                }
            }
            return strFileName;
        }
    }


    private void updateTracking(String ThreadName) {
        // clone the trackedFiles map, iterator through the clone and modify the
        // original to avoid iterator exceptions
        // is there a better way to do this?
        //System.out.println("Max Thread in updateTracking " + getSession().getMaxDirectoryPoolingThread());

        try {
            String strFile = "";
            strFile = GetFileFromQueue(ThreadName);
            if (strFile != "") {
                System.out.println("start processing for" + strFile);

                UpdateTrackingTask(strFile);


                synchronized (FileProcessingBlockingQueue) {
                    System.out.println("Remove from FileProcessingBlockingQueue" + strFile);
                    if(logger.isDebugEnabled())
                        logger.debug("Removeing from FileProcessingBlockingQueue" + strFile);
                    FileProcessingBlockingQueue.remove(strFile.replace(".processing", ".downloaded"));
                    SimpleLogMessage sm=new SimpleLogMessage();
                    sm.setLogMessage(" remove file " + strFile + "FileProcessingBlockingQueue ");
                    String pattern = Pattern.quote(System.getProperty("file.separator"));
                    sm.setFileName(strFile.split(pattern)[strFile.split(pattern).length-1]);
                    sm.setReceiverId(strFile.split(pattern)[strFile.split(pattern).length-2]);
                    logger.info(sm);
                    System.out.println(" remove file " + strFile + "FileProcessingBlockingQueue ");

                }

            }

        } catch (Exception exp) {

            System.out.println("Error occured in updateTracking" + exp.getMessage());
            logger.info("Error occured in updateTracking" + exp.getMessage());
        }

    }


    public void UpdateTrackingTask(String fileEntry) {

        System.out.println(" In UpdateTrackingTask for processing file " + fileEntry);
        //System.out.println("In UpdateTrackingTask"+fileEntry.getKey());
        // get the file and it's stored length
        File file = new File(fileEntry);

        try {
            // if the file no longer exists, remove it from the tracker
            if (!checkFile(file)) {
                System.out.println(" File not exists during check in UpdateTrackingTask for file " + fileEntry);

            } else {
                // if the file length has changed, update the tracker

                // if the file length has stayed the same, process the file
                // and stop tracking it
                try {

                    processFile(file);
                } catch (OpenAS2Exception e) {
                    System.out.println("Error occured in UpdateTrackingTask" + e.getMessage());
                    logger.error("Error occured in UpdateTrackingTask" + e.getMessage());
                    e.terminate();
                    try {
                        //IOUtilOld.handleError(file, errorDir);
                        IOUtilOld.handleError(file, errorDir);
                    } catch (OpenAS2Exception e1) {
                        logger.error("Error handling file error for file: " + file.getAbsolutePath(), e1);
                        System.out.println("Error occured in UpdateTrackingTask" + e1.getMessage());
                        //forceStop(e1);
                        return;
                    }
                }
            }
        } catch (Exception ex) {
            System.out.println("Error occured in UpdateTrackingTask" + ex.getMessage());
            logger.error("Error occured in UpdateTrackingTask", ex);

        }

    }


    protected void processFile(File file) throws OpenAS2Exception {

        if (logger.isDebugEnabled())
            logger.debug("processing " + file.getAbsolutePath());

        try {
            System.out.println("Document processing starts " + file.getAbsolutePath());
            SimpleLogMessage sm=new SimpleLogMessage();
            sm.setLogMessage("Document processing starts " + file.getAbsolutePath());
            sm.setFileName(file.getName());
            sm.setReceiverId(file.getParentFile().getName());
            logger.info(sm);
            processDocument(new FileInputStream(file), file.getName().replace(".processing", "").trim());
            System.out.println("Document processing completed " + file.getAbsolutePath());
            sm.setLogMessage("Document processing completed " + file.getAbsolutePath());
            logger.info(sm);
            try {
                IOUtilOld.deleteFile(file);
                System.out.println("Document processing deleted " + file.getAbsolutePath());
                sm.setLogMessage("Document processing deleted " + file.getAbsolutePath());
                logger.info(sm);
            } catch (IOException e) {
                throw new OpenAS2Exception("Failed to delete file handed off for processing:" + file.getAbsolutePath(), e);
            }
        } catch (FileNotFoundException e) {
            throw new OpenAS2Exception("Failed to process file as file not found:" + file.getAbsolutePath(), e);
        } catch (Exception e) {
            throw new OpenAS2Exception("Failed to process file due to exception:" + file.getAbsolutePath(), e);
        }
    }


    protected abstract Message createMessage();


}
