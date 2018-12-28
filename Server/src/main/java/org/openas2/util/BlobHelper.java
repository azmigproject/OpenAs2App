package org.openas2.util;

import com.microsoft.azure.storage.*;
import com.microsoft.azure.storage.blob.*;
import com.microsoft.azure.storage.queue.CloudQueue;
import com.microsoft.azure.storage.queue.CloudQueueMessage;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openas2.lib.dbUtils.ServersSettings;
import org.openas2.Constants;
import java.io.File;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.BlockingQueue;

public class BlobHelper {
	
	private Log logger = LogFactory.getLog("BlobHelper Class Process");

    public boolean UploadFileInBlob(String blobContainer,String blobName, byte[] byteContent) throws Exception {


        try {

                     // Retrieve storage account from connection-string.
            CloudStorageAccount storageAccount =
                    CloudStorageAccount.parse(Constants.STORAGEACCOUNTKEY);

            // Create the queue client.
            CloudBlobClient blobClient = storageAccount.createCloudBlobClient();

            // Retrieve a reference to a queue.
            CloudBlobContainer blockBlobContainer = blobClient.getContainerReference(blobContainer);

            // Create the queue if it doesn't already exist.
            //blockBlobContainer.createIfNotExists();
            CloudBlockBlob blockBlob = blockBlobContainer.getBlockBlobReference(blobName);
            blockBlob.uploadFromByteArray(byteContent, 100, byteContent.length);
        }
        catch (Exception e)
        {
            throw  e;
        }

        return true;
    }


    public boolean DownloadBlobInFile(String blobContainer,String blobName,String filePath, String fileName,HighPerformanceBlockingQueue fileQueue, CloudQueueMessage message, CloudQueue queue ) throws Exception {
        //final String storageConnectionString = "DefaultEndpointsProtocol=http;" + "AccountName=your_storage_account;" + "AccountKey=your_storage_account_key";

        try {



            // Retrieve storage account from connection-string.
            CloudStorageAccount storageAccount =
                    CloudStorageAccount.parse(Constants.STORAGEACCOUNTKEY);

            // Create the blob client.
            CloudBlobClient blobClient = storageAccount.createCloudBlobClient();

            // Retrieve a reference to a blob.
            CloudBlobContainer blockBlobContainer = blobClient.getContainerReference(blobContainer);
            CloudBlockBlob blockBlob = blockBlobContainer.getBlockBlobReference(blobName);
            //String fileDownloadPath = filePath + File.separator + fileName;
            String fileDownloadPath = filePath + fileName;
            blockBlob.downloadToFile(fileDownloadPath);
            
            if(logger.isDebugEnabled())
                logger.debug("Deleting File From Queue in GetMsgFromQueue method " + fileName );
            try {
                queue.deleteMessage(message);
            } catch (Exception exp) {
                System.out.println("Error occured in deleting queue mesage" + exp.getMessage());
                logger.error(exp);
            }
            
            File fl=new File(fileDownloadPath);
            //org.h2.store.fs.FileUtils.move(fileDownloadPath,fileDownloadPath+".downloaded");
            File NewFile=new File(fileDownloadPath+".downloaded");
            IOUtilOld.moveFile(fl,NewFile,false, true);
            synchronized (fileQueue) {

                    fileQueue.AddPath(fileDownloadPath + ".downloaded");

            }
            if(fl.exists() && fl.length()>0) {

                blockBlob.deleteIfExists();
            }
            fl=null;

        }
        catch (Exception e)
        {
            System.out.println("Error"+e.getMessage());
            System.out.println("blobName"+blobName);
            throw  e;
        }

        return true;
    }

    public boolean DeleteBlob(String blobContainer,String blobName) throws Exception {

        try {



            // Retrieve storage account from connection-string.
            CloudStorageAccount storageAccount =
                    CloudStorageAccount.parse(Constants.STORAGEACCOUNTKEY);

            // Create the blob client.
            CloudBlobClient blobClient = storageAccount.createCloudBlobClient();

            // Retrieve a reference to a blob.
            CloudBlobContainer blockBlobContainer = blobClient.getContainerReference(blobContainer);
            CloudBlockBlob blockBlob = blockBlobContainer.getBlockBlobReference(blobName);
            blockBlob.deleteIfExists();

        }
        catch (Exception e)
        {
            System.out.println("Error"+e.getMessage());
            System.out.println("blobName"+blobName);
            throw  e;
        }

        return true;
    }

    public OutputStream DownloadBlobOutStream(String blobContainer, String blobName, String filePath, String fileName) throws Exception {

        OutputStream outStream=null;
        try {


            // Retrieve storage account from connection-string.
            CloudStorageAccount storageAccount =
                    CloudStorageAccount.parse(Constants.STORAGEACCOUNTKEY);

            // Create the queue client.
            CloudBlobClient blobClient = storageAccount.createCloudBlobClient();

            // Retrieve a reference to a queue.
            CloudBlobContainer blockBlobContainer = blobClient.getContainerReference(blobContainer);
            CloudBlockBlob blockBlob = blockBlobContainer.getBlockBlobReference(blobName);
            //String fileDownloadPath = filePath + File.separator + fileName;
            String fileDownloadPath = filePath +  fileName;
            blockBlob.download(outStream);
            return outStream;
        }
        catch (Exception e)
        {
            throw  e;
        }
        finally {

                if(outStream!=null) {
                    outStream.close();
                    outStream.flush();
                    outStream=null;

                }

        }

    }
}
