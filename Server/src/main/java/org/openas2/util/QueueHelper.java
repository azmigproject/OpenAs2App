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
import java.util.concurrent.BlockingQueue;
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




    public boolean GetMsgFromQueue(String outDir, int NoOffiledownload,  HighPerformanceBlockingQueue fileQueue) {

        String queueMessage = "";
        String as2NewIdentifier = "";
        AzureUtil azureUtil = null;
        CloudStorageAccount storageAccount = null;
        CloudQueueClient queueClient = null;
        CloudQueue queue = null;
        boolean result=false;
        try {
            azureUtil = new AzureUtil();
            as2NewIdentifier = this.GetAS2Identifier(outDir);
            String queueName = this.GetQueueName(as2NewIdentifier);
            partner Partners = azureUtil.getActivePartnerBasedOnAs2Identifier(as2NewIdentifier);
            //System.out.println (as2NewIdentifier);
            if (Partners == null) {
                Exception e = new Exception("Partner Found Null for" + as2NewIdentifier + "query:" + "SELECT * FROM PARTNER WHERE LOWER(PARTNER.AS2Identifier)=\"" + as2NewIdentifier.toLowerCase());
                logger.error(e);
            }
            if (Partners != null && Partners.getIsActive()) {

                //// Retrieve storage account from connection-string.
                storageAccount= CloudStorageAccount.parse(Constants.STORAGEACCOUNTKEY);
                // Create the queue client.
                queueClient = storageAccount.createCloudQueueClient();
                // Retrieve a reference to a queue. storageAccount = CloudStorageAccount.parse(Constants.STORAGEACCOUNTKEY);
                queue = queueClient.getQueueReference(queueName);
                if (queue.exists()) {
                    //int queueCounter=0;
                    // System.out.println( "Approx msg in queue"+queue.getApproximateMessageCount() );
                    QueueRequestOptions queueReqOpt=new QueueRequestOptions();
                    queueReqOpt.setLocationMode(LocationMode.PRIMARY_THEN_SECONDARY);

                    Iterable<CloudQueueMessage> cloudMsgs = queue.retrieveMessages(NoOffiledownload, 600,queueReqOpt, null);
                    int intAccessCount=1;
                    //System.out.println("Try to Get Data Fromm Queue " + intAccessCount+" times");
                    //logger.info("Try to Get Data Fromm Queue " + intAccessCount+" times");
                    while(!((Iterable) cloudMsgs).iterator().hasNext() && intAccessCount>=3)
                    {
                        intAccessCount++;
                        cloudMsgs = queue.retrieveMessages(NoOffiledownload, 30,queueReqOpt, null);
                        //System.out.println("Try to Get Data Fromm Queue " + intAccessCount+" times");
                        //logger.info("Try to Get Data Fromm Queue " + intAccessCount+" times");
                    }

                    int msgCounter=0;

                    for (CloudQueueMessage message : cloudMsgs) {
                        msgCounter++;
                        result = true;
                        // Do processing for all messages in less than 5 minutes,
                        // deleting each message after processing.
                        try {
                            queueMessage = message.getMessageContentAsString();
                            System.out.println("In GetMsgFromQueueTask" + queueMessage);
                            if (queueMessage.contains("|__|")) {
                                String[] arr = queueMessage.split("\\|__\\|");
                                File file = new File(outDir + "\\" + arr[0]);
                                file.createNewFile();
                                FileWriter writer = new FileWriter(file);
                                writer.write(arr[1]);
                                writer.flush();
                                writer.close();
                                File NewFile=new File(outDir + "\\" + arr[0]+".downloaded");
                                IOUtilOld.moveFile(file,NewFile,false, true);
                                //org.h2.store.fs.FileUtils.move(outDir + "\\" + arr[0],);
                                synchronized (fileQueue) {


                                        fileQueue.AddPath(outDir + "\\" + arr[0] + ".downloaded");

                                }

                            } else { //if (queueMessage.contains("|_B_|"))
                                String[] arr = queueMessage.split("\\|_B_\\|");
                                //BlobHelper blob = new BlobHelper();
                                String blobName = GetBlobName(as2NewIdentifier, arr[0]);
                                BlobHelper blob = new BlobHelper();


                                if (blob.DownloadBlobInFile(Constants.BLOBCONTAINER, blobName, outDir, GetOriginalFileName(arr[0]),fileQueue)) {
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
                                System.out.println("Error occured in deleting queue mesage" + exp.getMessage());
                                logger.error(exp);
                            }
                        }

                    }
                    /*if(msgCounter==0)
                    {
                        System.out.println("message not found in queue " + queueName +"after "+intAccessCount+"tries");
                        logger.info("message not found in queue " + queueName +"after "+intAccessCount+"tries");
                    }*/

                }
                /*else
                {
                    System.out.println("queue not found " + queueName);
                    logger.info("queue not found " + queueName);
                }*/
                // Peek at the next message.
            }
            return result;

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

        return result;
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
