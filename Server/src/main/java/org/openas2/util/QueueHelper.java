package org.openas2.util;

import com.microsoft.azure.storage.*;
import com.microsoft.azure.storage.queue.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.openas2.lib.dbUtils.ServersSettings;


import java.io.File;
import java.io.FileWriter;
import java.util.List;

public class QueueHelper {
    private Log logger = LogFactory.getLog("QueueHelper Class Process");
    public boolean AddMsgToQueue(String queueName, String Msg) {

        try {
            AzureUtil azureUtil = new AzureUtil();
            azureUtil.init();
            List<ServersSettings> serverSettings = azureUtil.getServersSettings();
            ServersSettings serverSetting = serverSettings.get(0);
            // Retrieve storage account from connection-string.
            CloudStorageAccount storageAccount =
                    CloudStorageAccount.parse(serverSetting.getAzureStoragekey());
            // Create the queue client.
            CloudQueueClient queueClient = storageAccount.createCloudQueueClient();
            // Retrieve a reference to a queue.
            CloudQueue queue = queueClient.getQueueReference(queueName);
            // Peek at the next message.
            CloudQueueMessage message=new CloudQueueMessage(Msg);
            queue.addMessage(message);


        } catch (Exception e) {
            // Output the stack trace.
            e.printStackTrace();
            logger.error(e);
        }
        return true;
    }

    public boolean GetMsgFromQueue() {

        try {
            AzureUtil azureUtil = new AzureUtil();
            azureUtil.init();
            List<ServersSettings> serverSettings = azureUtil.getServersSettings();
            ServersSettings serverSetting = serverSettings.get(0);
            // Retrieve storage account from connection-string.
            CloudStorageAccount storageAccount =
                    CloudStorageAccount.parse(serverSetting.getAzureStoragekey());
            // Create the queue client.
            CloudQueueClient queueClient = storageAccount.createCloudQueueClient();
            // Retrieve a reference to a queue.
            CloudQueue queue = queueClient.getQueueReference("myqueue");
            // Peek at the next message.
            CloudQueueMessage peekedMessage = queue.retrieveMessage();
            // Output the message value.
            if (peekedMessage != null) {
                String message = peekedMessage.getMessageContentAsString();
            }
        } catch (Exception e) {
            // Output the stack trace.
            e.printStackTrace();
        }
        return true;
    }

    public boolean GetMsgFromQueue(String outDir) {

        try {
            AzureUtil azureUtil = new AzureUtil();
            azureUtil.init();
            String as2NewIdentifier = this.GetAS2Identifier(outDir);
            String queueName = this.GetQueueName(as2NewIdentifier);

            List<ServersSettings> serverSettings = azureUtil.getServersSettings();
            ServersSettings serverSetting = serverSettings.get(0);
            // Retrieve storage account from connection-string.
            CloudStorageAccount storageAccount = CloudStorageAccount.parse(serverSetting.getAzureStoragekey());
            // Create the queue client.
            CloudQueueClient queueClient = storageAccount.createCloudQueueClient();
            // Retrieve a reference to a queue.
            CloudQueue queue = queueClient.getQueueReference(queueName);
            queue.downloadAttributes();
            if(queue.getApproximateMessageCount() > 0)
            {
                for (CloudQueueMessage message : queue.retrieveMessages(4, 300, null, null)) {

                    String queueMessage = message.getMessageContentAsString();

                    if (queueMessage.contains("|__|")) {
                        String[] arr = queueMessage.split("\\|__\\|");
                        File file = new File(outDir + "\\" + arr[0]);
                        file.createNewFile();
                        FileWriter writer = new FileWriter(file);
                        writer.write(arr[1]);
                        writer.flush();
                        writer.close();
                    }
                    if (queueMessage.contains("|_B_|")) {
                        String[] arr = queueMessage.split("\\|_B_\\|");
                        //BlobHelper blob = new BlobHelper();
                        String blobName = GetBlobName(as2NewIdentifier, arr[0]);
                        BlobHelper blob = new BlobHelper();
                        //blob.UploadFileInBlob(serverSetting.getBlobContainerName(),"as10/outgoing/ship.xml","D:\\Sandeep_Work_2018\\data\\ServerFolder\\ship.xml");

                        blob.DownloadBlobInFile(serverSetting.getBlobContainerName(), blobName, outDir,arr[0]);
                    }

                    // Do processing for all messages in less than 5 minutes,
                    // deleting each message after processing.
                    queue.deleteMessage(message);
                }
            }
            // Peek at the next message.


        } catch (Exception e) {
            // Output the stack trace.
            e.printStackTrace();
        }
        return true;
    }

    public String GetBlobName(String as2Identifier, String fileName) {


        String blobName = as2Identifier+File.separator+"outgoing"+File.separator+fileName;
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

        String queueName = as2Identifier+"-"+"out";
        return queueName;
    }
}
