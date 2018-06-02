package org.openas2.util;

import com.microsoft.azure.storage.*;
import com.microsoft.azure.storage.queue.*;
import org.joda.time.DateTime;
import org.openas2.lib.dbUtils.ServersSettings;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
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

    public boolean GetMsgFromQueue(String queueName, String outDir, String as2Identifier) {

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
            for (CloudQueueMessage message : queue.retrieveMessages(4, 60, null, null)) {

                String queueMessage = message.getMessageContentAsString();
                if (queueMessage.contains("|__|")) {
                    String[] arr = queueMessage.split("\\|\\_\\_\\|");
                    File file = new File(outDir+"\\"+arr[0]);
                    file.createNewFile();
                    FileWriter writer = new FileWriter(file);
                    writer.write(arr[1]);
                    writer.flush();
                    writer.close();
                }
                if (queueMessage.contains("|_B_|")) {
                    String[] arr = queueMessage.split("\\|\\_B_\\|");
                    //BlobHelper blob = new BlobHelper();
                    String blobName = GetBlobName(as2Identifier,arr[0]);
                    BlobHelper blob = new BlobHelper();
                    //blob.UploadFileInBlob(serverSetting.getBlobContainerName(),"as10/outgoing/ship.xml","D:\\Sandeep_Work_2018\\data\\ServerFolder\\ship.xml");

                    blob.DownloadBlobInFile(serverSetting.getBlobContainerName(),blobName, outDir);
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
    public String GetBlobName(String as2Identifier, String fileName)
    {

        return as2Identifier+"outgoing"+fileName;
    }
}
