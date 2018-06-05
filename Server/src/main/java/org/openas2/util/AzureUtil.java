package org.openas2.util;
import org.openas2.lib.dbUtils.*;

import com.microsoft.azure.storage.*;
import com.microsoft.azure.storage.table.*;
import com.microsoft.azure.documentdb.*;
import org.openas2.lib.dbUtils.Properties;
import org.openas2.lib.dbUtils.partner;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONObject;
import org.json.JSONArray;
import com.google.gson.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;


public class AzureUtil {

    private static final String LOG_TABLE_NAME = "DBLog";
    private String PARTNER_TABLE_NAME = "Partner";
    private String PROFILE_TABLE_NAME = "Profile";
    private String PROPERTIES_TABLE_NAME="Properties";
    private String COMMANDS_TABLE_NAME="commands";
    private String PARTNERSHIPS_TABLE_NAME="Partnerships";
    private String COMMANDPROCESSOR_TABLE_NAME="CommandProcessors";
    private String SERVER_SETTINGS_TABLE_NAME = "ServerSettings";
    private String PROCESSOR_TABLE_NAME="Processor";
    private String CERTIFICATE_TABLE_NAME="certificates";
    //ToDo  check and replace the following code
    //private static final String COMMAND_TABLE_NAME="Command";
    private static final String CERTANDCOMMAND_TABLE_NAME="CertandCommandInfo";
    private static final String DEFAULT_PROCESSOR_NAME="DefaultProcessor";
    private static final String MODULE_TABLE_NAME="CommandProcessors";
    private static final String MULTICOMMAND_TABLE_NAME="Multicommand";
    private static final String SERVERMODULECLASS_TABLE_NAME="ServerModuleClasses";
    //ToDo  check and replace the following code
    private String COSMOS_DB_NAME = "NPTYAS2DB";

    private static final String STORAGE_CONNECTION_STRING = "UseDevelopmentStorage=true";
    //private static final String COSMOSDB_ENDPOINT = "https://localhost:8081/";
    //private static final String COSMOSDB_KEY = "C2y6yDjf5/R+ob0N8A7Cgv30VRDJIWEHLM+4QDU5DE2nQ9nDuVTqobD4b8mGGyPMbIZnqyMsEcaGQy67XIw/Jw==";
    private CloudTableClient tableClient;
    private DocumentClient documentClient;
    private Gson gson=null;
    private JSONObject configInfo=null;
    public void init() throws Exception
    {
        //ToDO Call API to ACCESS  THE Azure Info
        getLogDB();
        getNptyAS2DB();
        gson=new Gson();
    }

    public org.openas2.lib.dbUtils.Properties getProperties() {
        // Set some common query options
        FeedOptions queryOptions = new FeedOptions();
        queryOptions.setPageSize(-1);
        queryOptions.setEnableCrossPartitionQuery(true);
        org.openas2.lib.dbUtils.Properties propertiesInfo=new org.openas2.lib.dbUtils.Properties();

        String collectionLink = String.format("/dbs/%s/colls/%s", COSMOS_DB_NAME, PROPERTIES_TABLE_NAME);
        FeedResponse<Document> queryResults = this.documentClient.queryDocuments(collectionLink,
                "SELECT * FROM "+ PROPERTIES_TABLE_NAME, queryOptions);
        for (Document doc : queryResults.getQueryIterable()) {

           JSONObject objJSON=new JSONObject(doc.toJson());

            propertiesInfo.As2MessageIdFormat(objJSON.getString("as2_message_id_format"));
            propertiesInfo.SqlTimestampFormat(objJSON.getString("sql_timestamp_format"));
            propertiesInfo.LogDateFormat(objJSON.getString("log_date_format"));
            propertiesInfo.BasePath(objJSON.getString("basepath"));

        }
        return propertiesInfo;
    }

    public Certificates getCertificates() {
        // Set some common query options
        FeedOptions queryOptions = new FeedOptions();
        queryOptions.setPageSize(-1);
        queryOptions.setEnableCrossPartitionQuery(true);
        Certificates certificate=new Certificates() ;

        String collectionLink = String.format("/dbs/%s/colls/%s", COSMOS_DB_NAME, CERTIFICATE_TABLE_NAME);
        FeedResponse<Document> queryResults = this.documentClient.queryDocuments(collectionLink,
                "SELECT * FROM "+ CERTIFICATE_TABLE_NAME, queryOptions);
        for (Document doc : queryResults.getQueryIterable()) {

            JSONObject objJSON=new JSONObject(doc.toJson());
            certificate.setClassName(objJSON.getString("classname"));
            certificate.setFileName(objJSON.getString("filename"));
            certificate.setInterval(objJSON.getInt("interval"));
            certificate.setPassword(objJSON.getString("password"));
            certificate.setId(objJSON.getString("id"));


        }
        return certificate;
    }

    public Processor getProcessor() {
        // Set some common query options
        FeedOptions queryOptions = new FeedOptions();
        queryOptions.setPageSize(-1);
        queryOptions.setEnableCrossPartitionQuery(true);
        Processor processor=new Processor() ;

        String collectionLink = String.format("/dbs/%s/colls/%s", COSMOS_DB_NAME, PROCESSOR_TABLE_NAME);
        FeedResponse<Document> queryResults = this.documentClient.queryDocuments(collectionLink,
                "SELECT * FROM "+ PROCESSOR_TABLE_NAME, queryOptions);
        for (Document doc : queryResults.getQueryIterable()) {


            JSONObject objJSON=new JSONObject(doc.toJson());
            processor.setClassName(objJSON.getString("classname"));
            processor.setPendingMDN(objJSON.getString("pendingMDN"));
            processor.setPendingMDNInfo(objJSON.getString("pendingMDNinfo"));
            JSONArray objarr= objJSON.optJSONArray("Modules");
            module[] tempModule=new module[objarr.length()];
            for (int i=0;i<objarr.length();i++)
            {
                tempModule[i]=new module();
                JSONObject objTemp=objarr.getJSONObject(i);
                tempModule[i].setClassName(objTemp.getString("classname"));
                tempModule[i].setDelimiters(objTemp.getString("delimiters"));
                tempModule[i].setErrorDir(objTemp.getString("errordir"));
                tempModule[i].setErrorFormat(objTemp.getString("errorformat"));
                tempModule[i].setFileName(objTemp.getString("filename"));
                tempModule[i].setHeader(objTemp.getString("header"));
                tempModule[i].setInterval(objTemp.getInt("interval"));
                tempModule[i].setMimetype(objTemp.getString("mimetype"));
                tempModule[i].setOutboxDir(objTemp.getString("outboxdir"));
                tempModule[i].setPort(objTemp.getString("port"));
                tempModule[i].setProtocol(objTemp.getString("protocol"));
                tempModule[i].setResendDelay(objTemp.getInt("resenddelay"));
                tempModule[i].setResendDir(objTemp.getString("resenddir"));
                tempModule[i].setRetries( objTemp.getInt("retries"));
                tempModule[i].setTempDir(objTemp.getString("tempdir"));
                tempModule[i].setSendFileName(objTemp.getString("sendfilename"));
                tempModule[i].setFormat(objTemp.getString("format"));
                tempModule[i].setDefaults(objTemp.getString("defaults"));
                tempModule[i].setQueueName(objTemp.getString("queuename"));


            }
            processor.setModules(tempModule);


        }
        return processor;
    }

    public Command getCommands() {
        // Set some common query options
        FeedOptions queryOptions = new FeedOptions();
        queryOptions.setPageSize(-1);
        queryOptions.setEnableCrossPartitionQuery(true);
        Command cmd=new Command() ;

        String collectionLink = String.format("/dbs/%s/colls/%s", COSMOS_DB_NAME, COMMANDS_TABLE_NAME);
        FeedResponse<Document> queryResults = this.documentClient.queryDocuments(collectionLink,
                "SELECT * FROM "+ COMMANDS_TABLE_NAME, queryOptions);
        for (Document doc : queryResults.getQueryIterable()) {

            JSONObject objJSON=new JSONObject(doc.toJson());
            //cmd= gson.fromJson(doc.toJson(),Command.class);

        }
        return cmd;
    }









    public List<CommandProcessors> getCommandProcessors() {
        // Set some common query options
        FeedOptions queryOptions = new FeedOptions();
        queryOptions.setPageSize(-1);
        queryOptions.setEnableCrossPartitionQuery(true);
        List<CommandProcessors> commandProcessorInfo=new ArrayList<CommandProcessors>();

        String collectionLink = String.format("/dbs/%s/colls/%s", COSMOS_DB_NAME, COMMANDPROCESSOR_TABLE_NAME);
        FeedResponse<Document> queryResults = this.documentClient.queryDocuments(collectionLink,
                "SELECT * FROM "+ COMMANDPROCESSOR_TABLE_NAME, queryOptions);
        for (Document doc : queryResults.getQueryIterable()) {

            JSONObject objJSON=new JSONObject(doc.toJson());
            CommandProcessors  commandProcessor= new CommandProcessors();
            commandProcessor.setClassName(objJSON.getString("classname"));
            commandProcessor.setPassword(objJSON.getString("password"));
            commandProcessor.setPort(objJSON.getString("portId"));
            commandProcessor.setUserName(objJSON.getString("userId"));
            commandProcessorInfo.add(commandProcessor);
        }
        return commandProcessorInfo;
    }



    public List<ServersSettings> getServersSettings() {
        // Set some common query options
        FeedOptions queryOptions = new FeedOptions();
        queryOptions.setPageSize(-1);
        queryOptions.setEnableCrossPartitionQuery(true);
        List<ServersSettings> serverSettingsInfo=new ArrayList<ServersSettings>();

        String collectionLink = String.format("/dbs/%s/colls/%s", COSMOS_DB_NAME, SERVER_SETTINGS_TABLE_NAME);
        FeedResponse<Document> queryResults = this.documentClient.queryDocuments(collectionLink,
                "SELECT * FROM "+ SERVER_SETTINGS_TABLE_NAME, queryOptions);
        for (Document doc : queryResults.getQueryIterable()) {
            JSONObject objJSON=new JSONObject(doc.toJson());
            ServersSettings  serverSetting= new ServersSettings();
            serverSetting.setAllowHealthCheck(objJSON.getBoolean("AllowHealthCheck"));
            serverSetting.setAzureStoragekey(objJSON.getString("AzureStoragekey"));
            serverSetting.setBlobContainerName(objJSON.getString("BlobContainerName"));
            serverSetting.setAzureStoragekey(objJSON.getString("AzureStoragekey"));
            serverSetting.setMaxFileSize(objJSON.getInt("MaxFileSize_Queue"));
            serverSetting.setLogEmailID(objJSON.getBoolean("LogEmailID"));
            serverSetting.setMailServerEnableSSl(objJSON.getBoolean("MailServer_EnableSSL"));
            serverSetting.setMailServerPassword(objJSON.getString("MailServer_Password"));
            serverSetting.setMailServerPort(objJSON.getInt("MailServer_Port"));
            serverSetting.setMailServerSMTP(objJSON.getString("MailServer_MailServer"));
            serverSetting.setMailServerUserName(objJSON.getString("MailServer_UserName"));
            serverSettingsInfo.add(serverSetting);
        }
        return serverSettingsInfo;
    }





    public Commands getCommand() {
        // Set some common query options
        FeedOptions queryOptions = new FeedOptions();
        queryOptions.setPageSize(-1);
        queryOptions.setEnableCrossPartitionQuery(true);
        Commands commands=new Commands();

        String collectionLink = String.format("/dbs/%s/colls/%s", COSMOS_DB_NAME, COMMANDS_TABLE_NAME);
        FeedResponse<Document> queryResults = this.documentClient.queryDocuments(collectionLink,
                "SELECT * FROM "+ COMMANDS_TABLE_NAME, queryOptions);
        for (Document doc : queryResults.getQueryIterable()) {



            JSONObject objJSON=new JSONObject(doc.toJson());
            commands.setClassName(objJSON.getString("classname"));
            commands.setFileName(objJSON.getString("filename"));
            JSONArray objarr= objJSON.optJSONArray("commands");
            Multicommand[] tempCommand=new Multicommand[objarr.length()];
            for (int i=0;i<objarr.length();i++)
            {
                tempCommand[i]=new Multicommand();
                JSONObject objTemp=objarr.getJSONObject(i).getJSONObject("multicommands");
                tempCommand[i].setName(objTemp.getString("name"));
                tempCommand[i].setDescription(objTemp.getString("description"));
                JSONArray objCommandarr= objTemp.optJSONArray("command");
                Command[] availableCommands=new Command[objCommandarr.length()];
                for (int count=0;count<objCommandarr.length();count++) {
                    availableCommands[count]=new Command();
                    JSONObject objTempCommand=objCommandarr.getJSONObject(count);
                    availableCommands[count].setClassName(objTempCommand.getString("classname"));

                }

                tempCommand[i].setCommands(availableCommands);
            }
            commands.setMulticommands(tempCommand);

        }
        return commands;
    }



    public List<partner> getPartnerList() {
        // Set some common query options
        FeedOptions queryOptions = new FeedOptions();
        queryOptions.setPageSize(-1);
        queryOptions.setEnableCrossPartitionQuery(true);
        List<partner> partnerlist=new ArrayList<partner>();

        String collectionLink = String.format("/dbs/%s/colls/%s", COSMOS_DB_NAME, PARTNER_TABLE_NAME);
        FeedResponse<Document> queryResults = this.documentClient.queryDocuments(collectionLink,
                "SELECT * FROM "+ PARTNER_TABLE_NAME, queryOptions);
        for (Document doc : queryResults.getQueryIterable()) {

            JSONObject objJSON=new JSONObject(doc.toJson());
            partner partnerInfp=new partner();
            partnerInfp.setPublicCertificate(objJSON.getString("PublicCertificate"));
            partnerInfp.setPartnerUrl(objJSON.getString("PartnerUrl"));
            partnerInfp.setAS2Identifier(objJSON.getString("AS2Identifier"));
            partnerInfp.setEmailAddress(objJSON.getString("EmailAddress"));
            partnerInfp.setPartnerName(objJSON.getString("PartnerName"));
            //partnerInfp.setBlobFoldername(objJSON.getString("BlobFoldername"));
            partnerInfp.setConnectionTimeOutInSec(objJSON.getInt("ConnectionTimeOutInSec"));
            partnerInfp.setEnableAutomation(objJSON.getBoolean("EnableAutomation"));
            //partnerInfp.setCreatedBy(objJSON.getString("CreatedBy"));
            //partnerInfp.setCreatedOn(objJSON.getString("CreatedOn"));
            partnerInfp.setEncryptionAlgorithm(objJSON.getString("EncryptionAlgorithm"));
            partnerInfp.setSignatureAlgorithm(objJSON.getString("SignatureAlgorithm"));
            partnerInfp.setIncomingMessageRequireEncryption(objJSON.getBoolean("IncomingMessageRequireEncryption"));
            partnerInfp.setIncomingMessageRequireSignature(objJSON.getBoolean("IncomingMessageRequireSignature"));
            partnerInfp.setSignOutgoingMessage(objJSON.getBoolean("SignOutgoingMessage"));
            partnerInfp.setIncomingQueue(objJSON.getString("IncomingQueue"));
            partnerInfp.setOutgoingQueue(objJSON.getString("OutgoingQueue"));
            partnerInfp.setSentQueue(objJSON.getString("SentQueue"));
            partnerInfp.setInErrorQueue(objJSON.getString("IncommingErrorQueue"));
            partnerInfp.setOutErrorQueue(objJSON.getString("OutgoingErrorQueue"));
            partnerInfp.setIsFolderCreated(objJSON.getBoolean("IsFolderCreated"));
            partnerInfp.setIsMDNRequested(objJSON.getBoolean("IsMDNRequested"));
            partnerInfp.setISMDNSigned(objJSON.getBoolean("ISMDNSigned"));
            partnerInfp.setISMDNSigned(objJSON.getBoolean("IsSyncronous"));
            partnerInfp.setMaxAttempts(objJSON.getInt("MaxAttempts"));
            partnerInfp.setResendInterval(objJSON.getInt("ResendInterval"));
            partnerInfp.setRetryInterval(objJSON.getInt("RetryInterval"));
            partnerInfp.setIsMessageCompressed(objJSON.getBoolean("ISMessageCompressed"));
            partnerInfp.setSendFileNameInContentType(objJSON.getBoolean("SendFileNameInContentType"));
            partnerInfp.setOnPremIncomingDirName(objJSON.getString("OnPremIncomingDirName"));
            partnerInfp.setOnPremOutgoingDirName(objJSON.getString("OnPremOutgoingDirName"));
            partnerInfp.setOnPremSentDirName(objJSON.getString("OnPremSentDirName"));
            partnerInfp.setOnPremErrDirName(objJSON.getString("OnPremErrDirName"));
            partnerInfp.SetSSLEnabledProtocolsSSLv2(objJSON.getBoolean("SSLEnabledProtocolsSSLv2"));
            partnerInfp.SetSSLEnabledProtocolsSSLv3(objJSON.getBoolean("SSLEnabledProtocolsSSLv3"));
            partnerInfp.SetSSLEnabledProtocolsTLSv1(objJSON.getBoolean("SSLEnabledProtocolsTLSv1"));
            partnerInfp.SetSSLEnabledProtocolsTLSv11(objJSON.getBoolean("SSLEnabledProtocolsTLSv11"));
            partnerInfp.SetSSLEnabledProtocolsTLSv12(objJSON.getBoolean("SSLEnabledProtocolsTLSv12"));


            partnerlist.add(partnerInfp);
        }
        return partnerlist;
    }

    public Profile getProfile() {
        // Set some common query options
        FeedOptions queryOptions = new FeedOptions();
        queryOptions.setPageSize(-1);
        queryOptions.setEnableCrossPartitionQuery(true);
        List<Profile> profileInfo=new ArrayList<Profile>();
        String collectionLink = String.format("/dbs/%s/colls/%s", COSMOS_DB_NAME, PROFILE_TABLE_NAME);
        FeedResponse<Document> queryResults = this.documentClient.queryDocuments(collectionLink,
                "SELECT * FROM "+ PROFILE_TABLE_NAME, queryOptions);

        for (Document doc : queryResults.getQueryIterable()) {

            JSONObject objJSON=new JSONObject(doc.toJson());
            Profile profile=new Profile();
            profile.setAS2Idenitfier(objJSON.getString("AS2Idenitfier"));
            profile.setEemailAddress(objJSON.getString("EmailAddress"));
            profile.setAsynchronousMDNURL(objJSON.getString("AsynchronousMDNURL"));
            profile.setPrivateCertificate(objJSON.getString("PrivateCertificate"));
            profile.setPublicCertificate(objJSON.getString("PublicCertificate"));
            profile.setCertificatePassword(objJSON.getString("CertificatePassword"));
            profileInfo.add(profile);
        }
        return profileInfo.get(0);

    }

    private void getLogDB() throws Exception {


        CloudStorageAccount storageAccount =
                CloudStorageAccount.parse(STORAGE_CONNECTION_STRING);
        this.tableClient = storageAccount.createCloudTableClient();

        // Create the table if it doesn't exist.

        CloudTable cloudTable = this.tableClient.getTableReference(LOG_TABLE_NAME);
        cloudTable.createIfNotExists();
    }

    private void getNptyAS2DB() throws Exception {

        String dbInfo=getCosMOSDBINFO();
        JSONArray jsonArray=  new JSONArray(dbInfo);
        configInfo=jsonArray.getJSONObject(0);
        COSMOS_DB_NAME=configInfo.getString("CosmosDB");
        COMMANDS_TABLE_NAME=configInfo.getString("Commands");
        PROFILE_TABLE_NAME=configInfo.getString("Profile");
        SERVER_SETTINGS_TABLE_NAME=configInfo.getString("ServerSetting");
        PARTNER_TABLE_NAME=configInfo.getString("Partner");
        PROFILE_TABLE_NAME=configInfo.getString("Profile");
        CERTIFICATE_TABLE_NAME=configInfo.getString("Certificates");
        PARTNERSHIPS_TABLE_NAME=configInfo.getString("Partnerships");
        COMMANDPROCESSOR_TABLE_NAME=configInfo.getString("CommandProcessor");
        PROCESSOR_TABLE_NAME=configInfo.getString("Processor");
        PROPERTIES_TABLE_NAME=configInfo.getString("Properties");
            this.documentClient = new DocumentClient(configInfo.getString("CosmosDbEndPoint"),
                    configInfo.getString("CosmoDbKey"),
                    new ConnectionPolicy(),
                   ConsistencyLevel.Session);
    }

    private String getCosMOSDBINFO() throws Exception
    {
        String returnString="";
        URL url = new URL("http://nptyas2.westus.cloudapp.azure.com/api/partnerapi");//your url i.e fetch data from .
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");
        if (conn.getResponseCode() != 200) {
            throw new RuntimeException("Failed : HTTP Error code : "
                    + conn.getResponseCode());
        }
        InputStreamReader in = new InputStreamReader(conn.getInputStream());
        BufferedReader br = new BufferedReader(in);
        String output;
        while ((output = br.readLine()) != null) {
            returnString=returnString+output;
        }
        conn.disconnect();
        return returnString;
    }



}
