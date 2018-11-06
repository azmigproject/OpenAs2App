package org.openas2.processor.receiver;

import java.io.*;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ConcurrentHashMap;

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

public abstract class DirectoryPollingModule extends PollingModule
{
	public static final String PARAM_OUTBOX_DIRECTORY = "outboxdir";
	public static final String PARAM_FILE_EXTENSION_FILTER = "fileextensionfilter";
	private ConcurrentHashMap<String, Long> trackedFiles;
	private ConcurrentHashMap<String, Long> trackAddedToPoolFiles;
	private String outboxDir;
	private String errorDir;
	private String sentDir = null;
	static  int MAX_T = 15;
	static int MAX_QueueThread=15;


	private Log logger = LogFactory.getLog(DirectoryPollingModule.class.getSimpleName());

	public void init(Session session, Map<String, String> options) throws OpenAS2Exception
	{
		super.init(session, options);
		// Check all the directories are configured and actually exist on the file system
		try
		{
			MAX_T=session.getMaxDirectoryPoolingThread();
			MAX_QueueThread=session.getMaxQueuePoolingThread();
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
		try
		{
            updateTracking();
			 //int noOfFilesAllowedToDownload = 32;
            try {
                 int fileLength=IOUtilOld.getDirectoryFile(outboxDir).listFiles().length;
                if (fileLength < Constants.DOWNLLOADFILETHRESHOLD) {
                    //int noOfFilesAllowedToDownload = Math.min(Math.abs(Constants.DOWNLLOADFILETHRESHOLD - fileLength), 32);
                    int noOfFilesAllowedToDownload =32;
                     QueueHelper queueHelper = new QueueHelper();
                    queueHelper.GetMsgFromQueue(outboxDir, noOfFilesAllowedToDownload, MAX_QueueThread);
                }
            }
            catch(Exception e)
                {
                    System.out.println("Directry polled at"+(new Date()).toString() +"Exception"+e.getMessage()+"Max Count"+MAX_T);
                    logger.error("Unexpected error occurred polling directory for files to send: " + outboxDir, e);

                }
			scanDirectory(outboxDir);







				//scanDirectory(outboxDir);



			//// update tracking info. if a file is ready, process it

			/*
			ExecutorService PollPool = Executors.newFixedThreadPool(10);
			Runnable r1= new Runnable() {
				@Override
				public void run() {
					updateTracking();
				}
			};
			PollPool.execute(r1);



			Runnable r2= new Runnable() {
				@Override
				public void run() {
					try {
						scanDirectory(outboxDir);
					}
					catch (Exception ex)
					{
						System.out.println(" Error in Scan Directry at"+(new Date()).toString() +"Exception"+ex.getMessage()+"Max Count"+MAX_T);
						logger.error("Error in Scan Directry : " + outboxDir, ex);
//
					}
				}
			};
			PollPool.execute(r2);
			PollPool.isShutdown(); */



		}
		/*catch (OpenAS2Exception oae)
		{
			oae.terminate();
		}*/
		catch (Exception e)
		{
			System.out.println("Directry polled at"+(new Date()).toString() +"Exception"+e.getMessage()+"Max Count"+MAX_T);
			logger.error("Unexpected error occurred polling directory for files to send: " + outboxDir, e);
		}
	}




	protected void scanDirectory(final String directory) throws IOException, InvalidParameterException
	{

		try {

			File dir = IOUtilOld.getDirectoryFile(directory);

			//String extensionFilter = getParameter(PARAM_FILE_EXTENSION_FILTER, "");
			String extensionFilter = getParameter(PARAM_FILE_EXTENSION_FILTER, "downloaded");


			//if (dir.listFiles().length < Constants.DOWNLLOADFILETHRESHOLD) {
			//int noOfFilesAllowedToDownload = Math.min(Math.abs(Constants.DOWNLLOADFILETHRESHOLD - dir.listFiles().length), 32);
			//int noOfFilesAllowedToDownload =32;
			//queueHelper.GetMsgFromQueue(directory, noOfFilesAllowedToDownload);
			//}
			//final int noOfFilesAllowedToDownload =32;
			//QueueHelper queueHelper = new QueueHelper();
			//queueHelper.GetMsgFromQueue(directory, noOfFilesAllowedToDownload, MAX_QueueThread);
			/*
			if(!isOutGoingQueueRead) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						// code goes here.
						QueueHelper queueHelper = new QueueHelper();
						queueHelper.GetMsgFromQueue(directory, noOfFilesAllowedToDownload, MAX_QueueThread);
					}
				}).start();
				isOutGoingQueueRead=true;
			}*/

			// get a list of entries in the directory
			//File[] files = extensionFilter.length() > 0 ? IOUtilOld.getFiles(dir, extensionFilter) : dir.listFiles();
			File[] files = extensionFilter.length() > 0 ? getFilesBasedOnFilter(dir,extensionFilter) : dir.listFiles();
			if (files == null) {
				throw new InvalidParameterException("Error getting list of files in directory", this,
						PARAM_OUTBOX_DIRECTORY, dir.getAbsolutePath());
			}

			// iterator through each entry, and start tracking new files
			if (files.length > 0) {

				for (int i = 0; i < files.length; i++) {
					File currentFile = files[i];

					if (checkFile(currentFile)) {
						// start watching the file's size if it's not already being
						// watched
						trackFile(currentFile);
					}
				}
			}

			//System .out.println("TrackFileSize"+getTrackedFiles().size());
			//System .out.println("AddedToPoolTrackFileSize"+getAddedToPoolTrackedFiles().size());
		}
		catch (Exception exp) {

			System.out.println("Error occured in scanDirectory " + exp.getMessage());
		}
	}

	private File[] getFilesBasedOnFilter(File dir, final String extensionFilter)
	{
		try {
			return dir.listFiles(new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return name.toLowerCase().endsWith("." + extensionFilter);
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

	private void trackFile(File file)
	{
		//ConcurrentHashMap<String, Long> trackedFiles = getTrackedFiles();
		String filePath = file.getAbsolutePath();
		synchronized (this) {
			if (!getTrackedFiles().containsKey(filePath) && !getAddedToPoolTrackedFiles().containsKey(filePath)) {
				getTrackedFiles().putIfAbsent(filePath, file.length());
			}
		}
	}

	private void updateTracking() {
		// clone the trackedFiles map, iterator through the clone and modify the
		// original to avoid iterator exceptions
		// is there a better way to do this?
		//System.out.println("Max Thread in updateTracking " + getSession().getMaxDirectoryPoolingThread());
		ExecutorService pool = Executors.newFixedThreadPool(getSession().getMaxDirectoryPoolingThread());
		try {
			final ConcurrentHashMap<String, Long> trackedFiles = getTrackedFiles();
			Map<String, Long> trackedFilesClone = new ConcurrentHashMap<String, Long>(trackedFiles);


			for (final Map.Entry<String, Long> fileEntry : trackedFilesClone.entrySet()) {
				//System .out.println("TrackFileSize"+trackedFiles.size()+"trackedFilesClone"+trackedFilesClone.size());
				synchronized (this) {
				if(org.h2.store.fs.FileUtils.exists(fileEntry.getKey()) && org.h2.store.fs.FileUtils.size(fileEntry.getKey())>0 )
				if (!getAddedToPoolTrackedFiles().containsKey(fileEntry.getKey())) {
					Runnable r1 = new UpdateTrackingTask(fileEntry);

						trackAddedToPoolFiles.putIfAbsent(fileEntry.getKey(), fileEntry.getValue());
					pool.execute(r1);
					}

				}

				//trackedFiles.remove(fileEntry.getKey());
				//trackedFilesClone.remove(fileEntry.getKey());

			}
			pool.shutdown();
		} catch (Exception exp) {
			pool.shutdown();
			System.out.println("Error occured in updateTracking" + exp.getMessage());
		}

	}


	class UpdateTrackingTask implements Runnable
	{
		Map.Entry<String, Long> fileEntry;
		public UpdateTrackingTask(Map.Entry<String, Long> fileEntry )
		{
			this.fileEntry=fileEntry;
		}

		@Override
		public void run() {
			//System.out.println("In UpdateTrackingTask"+fileEntry.getKey());
			// get the file and it's stored length
			File file = new File(fileEntry.getKey());
			long fileLength = fileEntry.getValue().longValue();
			try {
				// if the file no longer exists, remove it from the tracker
				if (!checkFile(file)) {
					synchronized (this) {
						trackedFiles.remove(fileEntry.getKey());
						trackAddedToPoolFiles.remove(fileEntry.getKey());
					}
				} else {
					// if the file length has changed, update the tracker
					long newLength = file.length();
					if (newLength != fileLength) {
						synchronized (this) {
							trackedFiles.replace(fileEntry.getKey(), newLength);
							trackAddedToPoolFiles.remove(fileEntry.getKey());
						}
					} else {
						// if the file length has stayed the same, process the file
						// and stop tracking it
						try {
							File newFile =new File(fileEntry.getKey().replace(".downloaded",".processing"));
							file.renameTo(newFile);
							file=newFile;
							System.out.println("File Renamed" + file.toString());
							processFile(file);
						} catch (OpenAS2Exception e) {
							System.out.println("Error occured in UpdateTrackingTask" + e.getMessage());
							e.terminate();
							try {
								//IOUtilOld.handleError(file, errorDir);
								IOUtilOld.handleError(file, errorDir);
							} catch (OpenAS2Exception e1) {
								logger.error("Error handling file error for file: " + file.getAbsolutePath(), e1);
								System.out.println("Error occured in UpdateTrackingTask" + e.getMessage());
								forceStop(e1);
								return;
							}
						} finally {
							synchronized (this) {
								trackedFiles.remove(fileEntry.getKey());
								trackAddedToPoolFiles.remove(fileEntry.getKey());

							}
						}
					}
				}
			}
			catch (Exception ex)
			{
				System.out.println("Error occured in UpdateTrackingTask" + ex.getMessage());
			}

		}
	}



	protected void processFile(File file) throws OpenAS2Exception
	{

		if (logger.isInfoEnabled())
			logger.info("processing " + file.getAbsolutePath());

		try
		{
			System.out.println("Document processing starts " + file.getAbsolutePath());
			processDocument(new FileInputStream(file), file.getName().replace(".processing","").trim());
			System.out.println("Document processing completed " + file.getAbsolutePath());
			try
			{
				IOUtilOld.deleteFile(file);
				System.out.println("Document processing deleted " + file.getAbsolutePath());
			} catch (IOException e)
			{
				throw new OpenAS2Exception("Failed to delete file handed off for processing:" + file.getAbsolutePath(), e);
			}
		} catch (FileNotFoundException e)
		{
			throw new OpenAS2Exception("Failed to process file:" + file.getAbsolutePath(), e);
		}
	}

	protected void processFile(InputStream fileStream, String fileName ) throws OpenAS2Exception
	{

		if (logger.isInfoEnabled())
			logger.info("processing " + fileName);

		try
		{
			processDocument(fileStream, fileName);

		} catch (FileNotFoundException e)
		{
			throw new OpenAS2Exception("Failed to process file:" + fileName, e);
		}
	}

	protected abstract Message createMessage();

	private ConcurrentHashMap<String, Long> getTrackedFiles() {

		if (trackedFiles == null) {
			synchronized (this) {
				if (trackedFiles == null) {
					trackedFiles = new ConcurrentHashMap<String, Long>();
				}
			}
		}
		return trackedFiles;
	}

	private ConcurrentHashMap<String, Long> getAddedToPoolTrackedFiles() {

		if (trackAddedToPoolFiles == null) {
			synchronized (this) {
				if (trackAddedToPoolFiles == null) {
					trackAddedToPoolFiles = new ConcurrentHashMap<String, Long>();
				}
			}
		}
		return trackAddedToPoolFiles;
	}
}