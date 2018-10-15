package org.openas2.processor.receiver;

import java.io.*;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openas2.OpenAS2Exception;
import org.openas2.Session;
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
	private Map<String, Long> trackedFiles;
	private String outboxDir;
	private String errorDir;
	private String sentDir = null;

	private Log logger = LogFactory.getLog(DirectoryPollingModule.class.getSimpleName());

	public void init(Session session, Map<String, String> options) throws OpenAS2Exception
	{
		super.init(session, options);
		// Check all the directories are configured and actually exist on the file system
		try
		{
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

			// update tracking info. if a file is ready, process it
			//System.out.println("Directry polled at"+(new Date()).toString());

			updateTracking();

		/*	Thread thread = new Thread(){
				public void run(){

					updateTracking();
				}
			};

			thread.start();*/

			// scan the directory for new files
			scanDirectory(outboxDir);
		}
		/*catch (OpenAS2Exception oae)
		{
			oae.terminate();
		}*/
		catch (Exception e)
		{
			logger.error("Unexpected error occurred polling directory for files to send: " + outboxDir, e);
		}
	}

	protected void scanDirectory(String directory) throws IOException, InvalidParameterException
	{

		File dir = IOUtilOld.getDirectoryFile(directory);

		String extensionFilter = getParameter(PARAM_FILE_EXTENSION_FILTER, "");
		QueueHelper queueHelper = new QueueHelper();
		if(dir.listFiles().length < Constants.DOWNLLOADFILETHRESHOLD) {
            int noOfFilesAllowedToDownload = Math.min(Math.abs(Constants.DOWNLLOADFILETHRESHOLD - dir.listFiles().length ), 32);
            queueHelper.GetMsgFromQueue(directory, noOfFilesAllowedToDownload);
        }

		// get a list of entries in the directory
		File[] files = extensionFilter.length() > 0 ? IOUtilOld.getFiles(dir, extensionFilter) : dir.listFiles();
		if (files == null)
		{
			throw new InvalidParameterException("Error getting list of files in directory", this,
					PARAM_OUTBOX_DIRECTORY, dir.getAbsolutePath());
		}

		// iterator through each entry, and start tracking new files
		if (files.length > 0)
		{
			for (int i = 0; i < files.length; i++)
			{
				File currentFile = files[i];

				if (checkFile(currentFile))
				{
					// start watching the file's size if it's not already being
					// watched
					trackFile(currentFile);
				}
			}
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
		Map<String, Long> trackedFiles = getTrackedFiles();
		String filePath = file.getAbsolutePath();
		if (trackedFiles.get(filePath) == null)
		{
            trackedFiles.put(filePath, file.length());
        }
	}

    private void updateTracking()
    {
		// clone the trackedFiles map, iterator through the clone and modify the
		// original to avoid iterator exceptions
		// is there a better way to do this?
		final Map<String, Long> trackedFiles = getTrackedFiles();
		Map<String, Long> trackedFilesClone = new HashMap<String, Long>(trackedFiles);
		for (final Map.Entry<String, Long> fileEntry : trackedFilesClone.entrySet())
        {
			/*Thread thread = new Thread(){
				public void run(){
                    System.out.println("processFileInthread"+fileEntry.getKey());
					processFileInthread(fileEntry, trackedFiles);
				}
			};
			thread.start();*/


            // get the file and it's stored length
            File file = new File(fileEntry.getKey());
            long fileLength = fileEntry.getValue().longValue();

			// if the file no longer exists, remove it from the tracker
            if (!checkFile(file))
            {
                trackedFiles.remove(fileEntry.getKey());
            } else
            {
                // if the file length has changed, update the tracker
				long newLength = file.length();
                if (newLength != fileLength)
                {
                    trackedFiles.put(fileEntry.getKey(), new Long(newLength));
                } else
                {
                    // if the file length has stayed the same, process the file
					// and stop tracking it
                    try
                    {
                    	processFile(file);
                    }
                    catch (OpenAS2Exception e)
                    {
                        e.terminate();
                        try
                        {
                            IOUtilOld.handleError(file, errorDir);
                        } catch (OpenAS2Exception e1)
                        {
                            logger.error("Error handling file error for file: " + file.getAbsolutePath(), e1);
							forceStop(e1);
							return;
						}
                    } finally
                    {
                        trackedFiles.remove(fileEntry.getKey());
					}
				}
			}

		}

	}


	protected void processFileInthread(Map.Entry<String, Long> fileEntry, Map<String, Long> trackedFiles)
	{
		File file = new File(fileEntry.getKey());
		long fileLength = fileEntry.getValue().longValue();

		// if the file no longer exists, remove it from the tracker
		if (!checkFile(file))
		{
			trackedFiles.remove(fileEntry.getKey());
		} else
		{
			// if the file length has changed, update the tracker
			long newLength = file.length();
			if (newLength != fileLength)
			{
				trackedFiles.put(fileEntry.getKey(), new Long(newLength));
			} else
			{
				// if the file length has stayed the same, process the file
				// and stop tracking it
				try
				{
					 processFile(file);

				}
				catch (OpenAS2Exception e)
				{
					System.out.println(e.getMessage());
				    e.terminate();
					try
					{
						IOUtilOld.handleError(file, errorDir);
					} catch (OpenAS2Exception e1)
					{
						logger.error("Error handling file error for file: " + file.getAbsolutePath(), e1);
						forceStop(e1);
						return;
					}
				} finally
				{
					trackedFiles.remove(fileEntry.getKey());
				}
			}
		}

	}

	protected void processFile(File file) throws OpenAS2Exception
	{

		if (logger.isInfoEnabled())
			logger.info("processing " + file.getAbsolutePath());

		try
		{
			processDocument(new FileInputStream(file), file.getName());
			try
			{
				IOUtilOld.deleteFile(file);
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

    private Map<String, Long> getTrackedFiles()
    {
		if (trackedFiles == null)
		{
			trackedFiles = new HashMap<String, Long>();
		}
		return trackedFiles;
	}
}