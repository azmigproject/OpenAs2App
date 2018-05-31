package org.openas2.util;

import com.microsoft.azure.storage.*;
import com.microsoft.azure.storage.queue.*;
import org.joda.time.DateTime;
import org.openas2.lib.dbUtils.ServersSettings;

import java.io.File;
import java.util.List;

public class QueueHelper {
    public boolean AddMsgInQueue() {
        final String storageConnectionString = "DefaultEndpointsProtocol=http;" + "AccountName=your_storage_account;" + "AccountKey=your_storage_account_key";

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

            // Create the queue if it doesn't already exist.
            queue.createIfNotExists();
            CloudQueueMessage message = new CloudQueueMessage("Hello, World");
            queue.addMessage(message);
        } catch (Exception e) {
            // Output the stack trace.
            e.printStackTrace();
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

    public boolean GetMsgFromQueue(String queueName, String outDir) {

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

            for (CloudQueueMessage message : queue.retrieveMessages(20, 300, null, null)) {

                String queueMessage = message.getMessageContentAsString();
                if (queueMessage.contains("|__|")) {
                    String[] arr = queueMessage.split("|__|");
                    File file = new File(outDir);
                    file.getParentFile().mkdirs();
                    file.createNewFile();
                }
                if (queueMessage.contains("|_B_|")) {
                    String[] arr = queueMessage.split("|_B_|");
                    BlobHelper blob = new BlobHelper();
                    blob.DownloadBlobInFile("","", outDir);
                }
                // Do processing for all messages in less than 5 minutes,
                // deleting each message after processing.
                queue.deleteMessage(message);
            }

        } catch (Exception e) {
            // Output the stack trace.
            e.printStackTrace();
        }
        return true;
    }

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
        }
        return true;
    }
}
