package org.openas2.util;

import com.microsoft.azure.storage.*;
import com.microsoft.azure.storage.blob.*;
import org.openas2.lib.dbUtils.ServersSettings;
import java.util.List;
import java.io.*;
public class BlobHelper {

    public boolean UploadFileInBlob(String blobContainer,String blobName,String filePath) throws Exception {
        final String storageConnectionString = "DefaultEndpointsProtocol=http;" + "AccountName=your_storage_account;" + "AccountKey=your_storage_account_key";


            AzureUtil azureUtil = new AzureUtil();
            azureUtil.init();
            List<ServersSettings> serverSettings = azureUtil.getServersSettings();
            ServersSettings serverSetting = serverSettings.get(0);

            // Retrieve storage account from connection-string.
            CloudStorageAccount storageAccount =
                    CloudStorageAccount.parse(serverSetting.getAzureStoragekey());

            // Create the queue client.
            CloudBlobClient blobClient = storageAccount.createCloudBlobClient();

            // Retrieve a reference to a queue.
            CloudBlobContainer blockBlobContainer = blobClient.getContainerReference(blobContainer);

            // Create the queue if it doesn't already exist.
            blockBlobContainer.createIfNotExists();
            CloudBlockBlob blockBlob = blockBlobContainer.getBlockBlobReference(blobName);
            blockBlob.uploadFromFile(filePath);

        return true;
    }


    public boolean UploadFileInBlob(String blobContainer,String blobName, byte[] byteContent) throws Exception {
        final String storageConnectionString = "DefaultEndpointsProtocol=http;" + "AccountName=your_storage_account;" + "AccountKey=your_storage_account_key";


        AzureUtil azureUtil = new AzureUtil();
        azureUtil.init();
        List<ServersSettings> serverSettings = azureUtil.getServersSettings();
        ServersSettings serverSetting = serverSettings.get(0);

        // Retrieve storage account from connection-string.
        CloudStorageAccount storageAccount =
                CloudStorageAccount.parse(serverSetting.getAzureStoragekey());

        // Create the queue client.
        CloudBlobClient blobClient = storageAccount.createCloudBlobClient();

        // Retrieve a reference to a queue.
        CloudBlobContainer blockBlobContainer = blobClient.getContainerReference(blobContainer);

        // Create the queue if it doesn't already exist.
        blockBlobContainer.createIfNotExists();
        CloudBlockBlob blockBlob = blockBlobContainer.getBlockBlobReference(blobName);
        blockBlob.uploadFromByteArray(byteContent,100,byteContent.length);

        return true;
    }


    public boolean DownloadBlobInFile(String blobContainer,String blobName,String filePath) throws Exception {
        final String storageConnectionString = "DefaultEndpointsProtocol=http;" + "AccountName=your_storage_account;" + "AccountKey=your_storage_account_key";


        AzureUtil azureUtil = new AzureUtil();
        azureUtil.init();
        List<ServersSettings> serverSettings = azureUtil.getServersSettings();
        ServersSettings serverSetting = serverSettings.get(0);

        // Retrieve storage account from connection-string.
        CloudStorageAccount storageAccount =
                CloudStorageAccount.parse(serverSetting.getAzureStoragekey());

        // Create the queue client.
        CloudBlobClient blobClient = storageAccount.createCloudBlobClient();

        // Retrieve a reference to a queue.
        CloudBlobContainer blockBlobContainer = blobClient.getContainerReference(blobContainer);

        // Create the queue if it doesn't already exist.
        blockBlobContainer.createIfNotExists();
        CloudBlockBlob blockBlob = blockBlobContainer.getBlockBlobReference(blobName);
        blockBlob.downloadToFile(filePath);

        return true;
    }
}
