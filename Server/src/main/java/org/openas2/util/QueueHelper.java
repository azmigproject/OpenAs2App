package org.openas2.util;

import com.microsoft.azure.storage.*;
import com.microsoft.azure.storage.queue.*;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.openas2.Constants;
import org.openas2.lib.dbUtils.ServersSettings;
import org.openas2.lib.dbUtils.partner;
import org.openas2.partner.Partnership;


import java.io.File;
import java.io.FileWriter;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class QueueHelper {
    private Log logger = LogFactory.getLog("QueueHelper Class Process");
    private boolean busy;


    ////Used in BaseStorageModule
    public boolean isQueueProcessing()
    {
        return busy;
    }

    public void setQueueProcessing(boolean b)
    {
        busy = b;
    }
    public boolean AddMsgToQueue(String queueName, String Msg) {

        CloudStorageAccount storageAccount = null;
        CloudQueueClient queueClient = null;
        CloudQueue queue = null;
        try {

            storageAccount =
                    CloudStorageAccount.parse(Constants.STORAGEACCOUNTKEY);
            // Create the queue client.
            queueClient = storageAccount.createCloudQueueClient();
            // Retrieve a reference to a queue.
            queue = queueClient.getQueueReference(ConvertToCompatibleAzureName(queueName));
            if (queue.exists()) {
                // Peek at the next message.
                CloudQueueMessage message = new CloudQueueMessage(Msg);
                queue.addMessage(message);
            }


        } catch (Exception e) {
            // Output the stack trace.
            e.printStackTrace();
            logger.error(e);
        } finally {

            if (storageAccount != null) storageAccount = null;
            if (queueClient != null) queueClient = null;
            if (queue != null) queue = null;


        }
        return true;
    }


    public boolean GetMsgFromQueue(String outDir, int NoOffiledownload,int MaxQueueThread) {

        String queueMessage = "";
        String as2NewIdentifier = "";
        AzureUtil azureUtil = null;
        CloudStorageAccount storageAccount = null;
        CloudQueueClient queueClient = null;
        CloudQueue queue = null;
        try {
            azureUtil = new AzureUtil();
            as2NewIdentifier = this.GetAS2Identifier(outDir);
            String queueName = this.GetQueueName(as2NewIdentifier);
            partner Partners = azureUtil.getActivePartnerBasedOnAs2Identifier(as2NewIdentifier);
            //System.out.println (as2NewIdentifier);
            if (Partners == null) {
                Exception e = new Exception("Partner Found Null for" + as2NewIdentifier + "query:" + "SELECT * FROM PARTNER WHERE LOWER(PARTNER.AS2Identifier)=\"" + as2NewIdentifier.toLowerCase() + "/TotalThread"+MaxQueueThread);
                logger.error(e);
            }
            if (Partners != null && Partners.getIsActive()) {

                //// Retrieve storage account from connection-string.
                storageAccount = CloudStorageAccount.parse(Constants.STORAGEACCOUNTKEY);
                // Create the queue client.
                queueClient = storageAccount.createCloudQueueClient();
                // Retrieve a reference to a queue.
                queue = queueClient.getQueueReference(queueName);
                if (queue.exists()) {
                    //int queueCounter=0;
                    // System.out.println( "Approx msg in queue"+queue.getApproximateMessageCount() );

                    ExecutorService queuepool = Executors.newFixedThreadPool(MaxQueueThread);

                        for (CloudQueueMessage message : queue.retrieveMessages(NoOffiledownload, 600, null, null)) {

                            setQueueProcessing(true);
                            // Do processing for all messages in less than 5 minutes,
                            // deleting each message after processing.
                            Runnable r1 = new GetMsgFromQueueTask(message, outDir, as2NewIdentifier, queue);

                            queuepool.execute(r1);
                           

                            //++queueCounter;
                            // System.out.println( queueCounter+ " Queue mesage processed" );
                        }

                    queuepool.shutdown();


                    // System.out.println( queueCounter+ " Queue mesage processed" );
                }
                // Peek at the next message.
            }


        } catch (Exception e) {
            // Output the stack trace.
            System.out.println("Error occured" + e.getMessage());
            e.printStackTrace();
            if (!queueMessage.isEmpty()) {
                System.console().writer().write("FileName:" + queueMessage);
            }
            logger.error(e);
        } finally {
            if (storageAccount != null) storageAccount = null;
            if (queueClient != null) queueClient = null;
            if (queue != null) queue = null;
            if (azureUtil != null) {
                azureUtil.freeResources();
                azureUtil = null;

            }

        }

        return true;
    }


    class GetMsgFromQueueTask implements Runnable {
        CloudQueueMessage message;
        String outDir;
        String as2NewIdentifier;
        CloudQueue queue = null;
        String queueMessage;

        public GetMsgFromQueueTask(CloudQueueMessage message, String outDir, String as2NewIdentifier, CloudQueue queue) {
            System.out.println("In GetMsgFromQueueTask");
            try {
                this.message = message;
                this.outDir = outDir;
                this.as2NewIdentifier = as2NewIdentifier;
                this.queue = queue;
                this.queueMessage = message.getMessageContentAsString();
            } catch (Exception e) {
                // Output the stack trace.
                System.out.println("Error occured in GetMsgFromQueueTask" + e.getMessage());
                e.printStackTrace();
                if (!queueMessage.isEmpty()) {
                    System.console().writer().write("FileName:" + queueMessage);
                }
                logger.error(e);
            }

        }

        @Override
        public void run() {

            try {
                System.out.println("In GetMsgFromQueueTask" + queueMessage);
                if (queueMessage.contains("|__|")) {
                    String[] arr = queueMessage.split("\\|__\\|");
                    File file = new File(outDir + "\\" + arr[0]);
                    file.createNewFile();
                    FileWriter writer = new FileWriter(file);
                    writer.write(arr[1]);
                    writer.flush();
                    writer.close();
                    org.h2.store.fs.FileUtils.move(outDir + "\\" + arr[0],outDir + "\\" + arr[0]+".downloaded");

                } else { //if (queueMessage.contains("|_B_|"))
                    String[] arr = queueMessage.split("\\|_B_\\|");
                    //BlobHelper blob = new BlobHelper();
                    String blobName = GetBlobName(as2NewIdentifier, arr[0]);
                    BlobHelper blob = new BlobHelper();
                    ////blob.UploadFileInBlob(serverSetting.getBlobContainerName(),"as10/outgoing/ship.xml","D:\\Sandeep_Work_2018\\data\\ServerFolder\\ship.xml");

                    if (blob.DownloadBlobInFile(Constants.BLOBCONTAINER, blobName, outDir, GetOriginalFileName(arr[0]))) {
                        blob.DeleteBlob(Constants.BLOBCONTAINER, blobName);
                    }
                }

            } catch (Exception e) {
                // Output the stack trace.
                System.out.println("Error occured" + e.getMessage());
                e.printStackTrace();
                if (!queueMessage.isEmpty()) {
                    System.console().writer().write("FileName:" + queueMessage);
                }
                logger.error(e);
            } finally {
                try {
                    queue.deleteMessage(message);
                } catch (Exception exp) {
                    logger.error(exp);
                }
            }
        }
    }



    public String GetOriginalFileName(String fileName) {


        String [] newFileName = fileName.split("-_-");
        return newFileName[0]+"."+ FilenameUtils.getExtension(fileName);
    }

    public String GetBlobName(String as2Identifier, String fileName) {


        String blobName = ConvertToCompatibleAzureName(as2Identifier.toLowerCase())+"/outgoing/"+fileName;
        return blobName;
    }
   public String GetAS2Identifier(String outDir)
   {

       String arr [] = outDir.split("\\\\");
       String as2Identifier = arr[arr.length-1];
       return as2Identifier;
   }
    public String GetQueueName(String as2Identifier)
    {

        String queueName = ConvertToCompatibleAzureName(as2Identifier.toLowerCase())+"-"+"out";
        return queueName;
    }
    public String ConvertToCompatibleAzureName(String strName)
    {
       return  strName.replaceAll("[^a-zA-Z0-9-]" , "-");
    }
}
