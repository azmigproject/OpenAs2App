package org.openas2.util;
import org.openas2.Constants;
import org.openas2.lib.dbUtils.*;

import com.microsoft.azure.storage.*;
import com.microsoft.azure.storage.table.*;
import com.microsoft.azure.documentdb.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.json.JSONObject;
import org.json.JSONArray;
import com.google.gson.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;


import org.openas2.XMLSession;

public class AzureUtil {

    public  String LOG_TABLE_NAME = "DBLog";
    private String PARTNER_TABLE_NAME = "Partner";
    private String PROFILE_TABLE_NAME = "Profile";
    private String LAST_UPDATED_TIME_STAMP = "LastUpdatedTimeStamp";
    private String PROPERTIES_TABLE_NAME="Properties";
    private String COMMANDS_TABLE_NAME="commands";
    private String NPTYAS2DEFAULTSETTINGS_TABLE_NAME="NPTYAS2DefaultSettings";
    private String COMMANDPROCESSOR_TABLE_NAME="CommandProcessors";
    private String SERVER_SETTINGS_TABLE_NAME = "ServerSettings";
    private String PROCESSOR_TABLE_NAME="Processor";
    private String CERTIFICATE_TABLE_NAME="certificates";
    public String COSMOS_DB_NAME = "NPTYAS2DB";
    public String STORAGE_CONNECTION_STRING = "UseDevelopmentStorage=true";
    private CloudTableClient tableClient;
    private DocumentClient documentClient;
    private JSONObject configInfo=null;
    private  String CosmosDBAPI=Constants.APIURL;



    public void init() throws Exception
    {
        //ToDO Call API to ACCESS  THE Azure Info
        CosmosDBAPI=Constants.APIURL;
       getNptyAS2DB(CosmosDBAPI);
        getLogDB();
    Constants.AllProfiles=getAllProfile();
    }

    public void freeResources()
    {
        //ToDO Call API to ACCESS  THE Azure Info
      if(documentClient!=null) {
          documentClient.close();
          documentClient = null;
      }
       if(tableClient!=null){   tableClient = null;}


    }


    public void init(boolean loadFromSavedValue) throws Exception
    {
        //ToDO Call API to ACCESS  THE Azure Info
        CosmosDBAPI=Constants.APIURL;
        if(loadFromSavedValue)

        {
            getNptyAS2DB(Constants.APIDataInJASON  );
            if(Constants.LastUpdateTimeStamp=="") {
            Constants.LastUpdateTimeStamp=getLastUpdatedTimeStamp();
            }
        }
        else

        {
            getNptyAS2DB(CosmosDBAPI);
        }
        getLogDB();
        //gson=new Gson();
    }

    public org.openas2.lib.dbUtils.Properties getProperties() {
        // Set some common query options
        FeedOptions queryOptions = new FeedOptions();
        queryOptions.setPageSize(-1);
        queryOptions.setEnableCrossPartitionQuery(true);
         org.openas2.lib.dbUtils.Properties propertiesInfo=new org.openas2.lib.dbUtils.Properties();

        String collectionLink = String.format("/dbs/%s/colls/%s", COSMOS_DB_NAME, NPTYAS2DEFAULTSETTINGS_TABLE_NAME);
        FeedResponse<Document> queryResults = this.documentClient.queryDocuments(collectionLink,
                "SELECT * FROM "+ NPTYAS2DEFAULTSETTINGS_TABLE_NAME+" WHERE "+NPTYAS2DEFAULTSETTINGS_TABLE_NAME+".id='"+PROPERTIES_TABLE_NAME+"'", queryOptions);
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

        String collectionLink = String.format("/dbs/%s/colls/%s", COSMOS_DB_NAME, NPTYAS2DEFAULTSETTINGS_TABLE_NAME);
        FeedResponse<Document> queryResults = this.documentClient.queryDocuments(collectionLink,
                "SELECT * FROM "+ NPTYAS2DEFAULTSETTINGS_TABLE_NAME+" WHERE "+NPTYAS2DEFAULTSETTINGS_TABLE_NAME+".id='"+CERTIFICATE_TABLE_NAME+"'" , queryOptions);
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

        String collectionLink = String.format("/dbs/%s/colls/%s", COSMOS_DB_NAME, NPTYAS2DEFAULTSETTINGS_TABLE_NAME);
        FeedResponse<Document> queryResults = this.documentClient.queryDocuments(collectionLink,
                "SELECT * FROM "+NPTYAS2DEFAULTSETTINGS_TABLE_NAME+" WHERE "+NPTYAS2DEFAULTSETTINGS_TABLE_NAME+".id='"+ PROCESSOR_TABLE_NAME+"'", queryOptions);
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
                if(tempModule[i].getProtocol().trim().equalsIgnoreCase("https") ) {

                    tempModule[i].setSSLProtocol(objTemp.getString("ssl_protocol"));
                    tempModule[i].setSSLKeyStore(objTemp.getString("ssl_keystore"));
                    tempModule[i].setSSLKeyStorePassword(objTemp.getString("ssl_keystore_password"));

                }


            }
            processor.setModules(tempModule);


        }
        return processor;
    }

   /* public Command getCommands() {
        // Set some common query options
        FeedOptions queryOptions = new FeedOptions();
        queryOptions.setPageSize(-1);
        queryOptions.setEnableCrossPartitionQuery(true);
        Command cmd=new Command() ;

        String collectionLink = String.format("/dbs/%s/colls/%s", COSMOS_DB_NAME, NPTYAS2DEFAULTSETTINGS_TABLE_NAME);
        FeedResponse<Document> queryResults = this.documentClient.queryDocuments(collectionLink,
                "SELECT * FROM "+NPTYAS2DEFAULTSETTINGS_TABLE_NAME+" WHERE c.id='"+ COMMANDS_TABLE_NAME+"'", queryOptions);
        for (Document doc : queryResults.getQueryIterable()) {

            JSONObject objJSON=new JSONObject(doc.toJson());


        }
        return cmd;
    }*/









    public List<CommandProcessors> getCommandProcessors() {
        // Set some common query options
        FeedOptions queryOptions = new FeedOptions();
        queryOptions.setPageSize(-1);
        queryOptions.setEnableCrossPartitionQuery(true);
        List<CommandProcessors> commandProcessorInfo=new ArrayList<CommandProcessors>();

        String collectionLink = String.format("/dbs/%s/colls/%s", COSMOS_DB_NAME, NPTYAS2DEFAULTSETTINGS_TABLE_NAME);
        FeedResponse<Document> queryResults = this.documentClient.queryDocuments(collectionLink,
                "SELECT * FROM "+NPTYAS2DEFAULTSETTINGS_TABLE_NAME+" WHERE "+NPTYAS2DEFAULTSETTINGS_TABLE_NAME+".id='"+ COMMANDPROCESSOR_TABLE_NAME+"'", queryOptions);
        for (Document doc : queryResults.getQueryIterable()) {

            JSONObject objTemp=new JSONObject(doc.toJson());
            JSONArray objarr= objTemp.optJSONArray("Processor");
            for (int i=0;i<objarr.length();i++) {
                JSONObject objJSON=objarr.getJSONObject(i);
                CommandProcessors commandProcessor = new CommandProcessors();
                commandProcessor.setClassName(objJSON.getString("classname"));
                commandProcessor.setPassword(objJSON.getString("password"));
                commandProcessor.setPort(objJSON.getString("portId"));
                commandProcessor.setUserName(objJSON.getString("userId"));
                commandProcessorInfo.add(commandProcessor);
            }
        }
        return commandProcessorInfo;
    }



    public List<ServersSettings> getServersSettings() {
        // Set some common query options
        FeedOptions queryOptions = new FeedOptions();
        queryOptions.setPageSize(-1);
        queryOptions.setEnableCrossPartitionQuery(true);
        List<ServersSettings> serverSettingsInfo=new ArrayList<ServersSettings>();

        String collectionLink = String.format("/dbs/%s/colls/%s", COSMOS_DB_NAME, NPTYAS2DEFAULTSETTINGS_TABLE_NAME);
        FeedResponse<Document> queryResults = this.documentClient.queryDocuments(collectionLink,
                "SELECT * FROM "+NPTYAS2DEFAULTSETTINGS_TABLE_NAME+" WHERE "+NPTYAS2DEFAULTSETTINGS_TABLE_NAME+".id='"+  SERVER_SETTINGS_TABLE_NAME+"'", queryOptions);
        for (Document doc : queryResults.getQueryIterable()) {
            JSONObject objJSON=new JSONObject(doc.toJson());
            ServersSettings  serverSetting= new ServersSettings();
            serverSetting.setAllowHealthCheck(objJSON.getBoolean("AllowHealthCheck"));
            serverSetting.setAzureStoragekey(objJSON.getString("AzureStorageKey"));
            Constants.STORAGEACCOUNTKEY=serverSetting.getAzureStoragekey();
            serverSetting.setBlobContainerName(objJSON.getString("BlobContainerName"));
            Constants.BLOBCONTAINER=serverSetting.getBlobContainerName();
            serverSetting.setMaxFileSize(objJSON.getInt("MaxFileSize_Queue"));
            serverSetting.setLogInEmail(objJSON.getBoolean("LogInEmail"));
            serverSetting.setLogEmailID(objJSON.getString("LogEmailID"));
            serverSetting.setMailServerEnableSSL(objJSON.getBoolean("MailServerEnableSSL"));
            serverSetting.setMailServerPassword(objJSON.getString("MailServerPassword"));
            serverSetting.setMailServerPort(objJSON.getInt("MailServerPort"));
            serverSetting.setMailServerSMTP(objJSON.getString("MailServerSMTP"));
            serverSetting.setMailServerUserName(objJSON.getString("MailServerUserName"));
            serverSettingsInfo.add(serverSetting);
        }
        return serverSettingsInfo;
    }


    public String getAzureStorageKey() {
        // Set some common query options
        FeedOptions queryOptions = new FeedOptions();
        queryOptions.setPageSize(-1);
        queryOptions.setEnableCrossPartitionQuery(true);
         String collectionLink = String.format("/dbs/%s/colls/%s", COSMOS_DB_NAME, NPTYAS2DEFAULTSETTINGS_TABLE_NAME);
        FeedResponse<Document> queryResults = this.documentClient.queryDocuments(collectionLink,
                "SELECT * FROM "+NPTYAS2DEFAULTSETTINGS_TABLE_NAME+" WHERE "+NPTYAS2DEFAULTSETTINGS_TABLE_NAME+".id='"+ SERVER_SETTINGS_TABLE_NAME+"'", queryOptions);
        String strResult="";
        for (Document doc : queryResults.getQueryIterable()) {
            JSONObject objJSON=new JSONObject(doc.toJson());
            strResult=objJSON.getString("AzureStorageKey");

        }
        return strResult ;
    }

    public String getLastUpdatedTimeStamp()
    {
        FeedOptions queryOptions = new FeedOptions();
        queryOptions.setPageSize(-1);
        queryOptions.setEnableCrossPartitionQuery(true);
        String LastUpdatedTimeStamp="";

        String collectionLink = String.format("/dbs/%s/colls/%s", COSMOS_DB_NAME, NPTYAS2DEFAULTSETTINGS_TABLE_NAME);
        FeedResponse<Document> queryResults = this.documentClient.queryDocuments(collectionLink,
                "SELECT * FROM "+NPTYAS2DEFAULTSETTINGS_TABLE_NAME+" WHERE "+NPTYAS2DEFAULTSETTINGS_TABLE_NAME+".id='"+LAST_UPDATED_TIME_STAMP+"'", queryOptions);

        for (Document doc : queryResults.getQueryIterable()) {



            JSONObject objJSON=new JSONObject(doc.toJson());
            LastUpdatedTimeStamp=objJSON.getString("LastUpdatedDate");


        }
        return LastUpdatedTimeStamp;
    }


    public Commands getCommand() {
        // Set some common query options
        FeedOptions queryOptions = new FeedOptions();
        queryOptions.setPageSize(-1);
        queryOptions.setEnableCrossPartitionQuery(true);
        Commands commands=new Commands();

        String collectionLink = String.format("/dbs/%s/colls/%s", COSMOS_DB_NAME, NPTYAS2DEFAULTSETTINGS_TABLE_NAME);
        FeedResponse<Document> queryResults = this.documentClient.queryDocuments(collectionLink,
                "SELECT * FROM "+NPTYAS2DEFAULTSETTINGS_TABLE_NAME+" WHERE "+NPTYAS2DEFAULTSETTINGS_TABLE_NAME+".id='"+COMMANDS_TABLE_NAME+"'", queryOptions);

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

            partner partnerInfp=GetPartnerFromDocument(doc);


            partnerlist.add(partnerInfp);
        }
        Constants.CURRENTPARTNERLIST=partnerlist;
        return partnerlist;
    }


    public partner getActivePartnerBasedOnAs2Identifier(String strAs2Identifier) {
        // Set some common query options

        partner partnerInfp=null;

        for(partner d : Constants.CURRENTPARTNERLIST){
            if(d.getAS2Identifier().toLowerCase().trim().equalsIgnoreCase(strAs2Identifier.toLowerCase().trim()))
            {
                partnerInfp=d;
                break;
            }
            else {
                //System.out.println ("No Partner matched" + strAs2Identifier.toLowerCase().trim()+" "+ d.getAS2Identifier().toLowerCase().trim()+"="+ Constants.CURRENTPARTNERLIST.size()  );

            }
            //something here
        }

        if(partnerInfp==null)
        {
            System.out.println ("No Partner Found" + strAs2Identifier+ Constants.CURRENTPARTNERLIST.size()  );
        }
        return partnerInfp;
    }

private partner GetPartnerFromDocument(Document doc)
{
    JSONObject objJSON=new JSONObject(doc.toJson());
    partner partnerInfp=new partner();
    if(objJSON.has("IsActive")) {
        partnerInfp.setIsActive(objJSON.getBoolean("IsActive"));
    }
    else
    {
        partnerInfp.setIsActive(false);
    }
    partnerInfp.setPublicCertificate(objJSON.getString("PublicCertificate"));
    partnerInfp.setPartnerUrl(objJSON.getString("PartnerUrl"));
    partnerInfp.setAS2Identifier(objJSON.getString("AS2Identifier"));
    partnerInfp.setEmailAddress(objJSON.getString("EmailAddress"));
    partnerInfp.setPartnerName(objJSON.getString("PartnerName"));
    partnerInfp.setConnectionTimeOutInSec(objJSON.getInt("ConnectionTimeOutInSec"));
    partnerInfp.setEnableAutomation(objJSON.getBoolean("EnableAutomation"));
    partnerInfp.setEncryptionAlgorithm(objJSON.getString("EncryptionAlgorithm"));
    partnerInfp.setSignatureAlgorithm(objJSON.getString("SignatureAlgorithm"));
    partnerInfp.setIncomingMessageRequireEncryption(objJSON.getBoolean("IncomingMessageRequireEncryption"));
    partnerInfp.setIncomingMessageRequireSignature(objJSON.getBoolean("IncomingMessageRequireSignature"));
    partnerInfp.setSignOutgoingMessage(objJSON.getBoolean("SignOutgoingMessage"));
    partnerInfp.setEncryptOutgoingMessage(objJSON.getBoolean("EncryptOutgoingMessage"));
    partnerInfp.setIncomingQueue(objJSON.getString("IncomingQueue"));
    partnerInfp.setOutgoingQueue(objJSON.getString("OutgoingQueue"));
    partnerInfp.setSentQueue(objJSON.getString("SentQueue"));
    partnerInfp.setInErrorQueue(objJSON.getString("IncomingErrorQueue"));
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
    return partnerInfp;

}

    public Profile getProfileById(String as2Identifier) {
        // Set some common query options
        FeedOptions queryOptions = new FeedOptions();
        queryOptions.setPageSize(-1);
        queryOptions.setEnableCrossPartitionQuery(true);
        List<Profile> profileInfo=new ArrayList<Profile>();
        String collectionLink = String.format("/dbs/%s/colls/%s", COSMOS_DB_NAME, PROFILE_TABLE_NAME);
        FeedResponse<Document> queryResults = this.documentClient.queryDocuments(collectionLink,
                "SELECT * FROM "+PROFILE_TABLE_NAME+" WHERE "+PROFILE_TABLE_NAME+".AS2Identifier='"+as2Identifier+"'", queryOptions);

        for (Document doc : queryResults.getQueryIterable()) {

            JSONObject objJSON=new JSONObject(doc.toJson());
            Profile profile=new Profile();
            profile.setAS2Idenitfier(objJSON.getString("AS2Identifier"));
            profile.setEemailAddress(objJSON.getString("EmailAddress"));
            profile.setAsynchronousMDNURL(objJSON.getString("AsynchronousMDNURL"));
            profile.setPrivateCertificate(objJSON.getString("PrivateCertificate"));
            profile.setIsMainProfile((objJSON.getBoolean("IsMainProfile")));
            profile.setDescription(objJSON.getString("Description"));
            profile.setOnPremHomeDirectory(objJSON.getString("OnPremHomeDirectory"));
            profile.setOnPremHomeDirectoryLinux(objJSON.getString("OnPremHomeDirectoryLinux"));
            profile.setDataFolder(objJSON.getString("DataFolder"));
            profile.setPublicCertificate(objJSON.getString("PublicCertificate"));
            profile.setCertificatePassword(objJSON.getString("CertificatePassword"));
            profileInfo.add(profile);
        }
        //System.out.println("PublicCertificate"+profileInfo.get(0).getPublicCertificate());
        //System.out.println("PrivateCertificate"+profileInfo.get(0).getPrivateCertificate());
        return profileInfo.get(0);

    }

    public List<Profile> getAllProfile() {
        // Set some common query options
        FeedOptions queryOptions = new FeedOptions();
        queryOptions.setPageSize(-1);
        queryOptions.setEnableCrossPartitionQuery(true);
        List<Profile> profileInfo=new ArrayList<Profile>();
        String collectionLink = String.format("/dbs/%s/colls/%s", COSMOS_DB_NAME, PROFILE_TABLE_NAME);
        FeedResponse<Document> queryResults = this.documentClient.queryDocuments(collectionLink,
                "SELECT * FROM "+PROFILE_TABLE_NAME, queryOptions);

        for (Document doc : queryResults.getQueryIterable()) {

            JSONObject objJSON=new JSONObject(doc.toJson());
            Profile profile=new Profile();
            profile.setAS2Idenitfier(objJSON.getString("AS2Identifier"));
            profile.setEemailAddress(objJSON.getString("EmailAddress"));
            profile.setAsynchronousMDNURL(objJSON.getString("AsynchronousMDNURL"));
            profile.setPrivateCertificate(objJSON.getString("PrivateCertificate"));
            profile.setIsMainProfile((objJSON.getBoolean("IsMainProfile")));
            profile.setDescription(objJSON.getString("Description"));
            profile.setOnPremHomeDirectory(objJSON.getString("OnPremHomeDirectory"));
            profile.setOnPremHomeDirectoryLinux(objJSON.getString("OnPremHomeDirectoryLinux"));
            profile.setDataFolder(objJSON.getString("DataFolder"));
            profile.setPublicCertificate(objJSON.getString("PublicCertificate"));
            profile.setCertificatePassword(objJSON.getString("CertificatePassword"));
            profileInfo.add(profile);
        }
        //System.out.println("PublicCertificate"+profileInfo.get(0).getPublicCertificate());
        //System.out.println("PrivateCertificate"+profileInfo.get(0).getPrivateCertificate());

        return profileInfo;

    }

    public Profile getMainProfile() {
        // Set some common query options
        FeedOptions queryOptions = new FeedOptions();
        queryOptions.setPageSize(-1);
        queryOptions.setEnableCrossPartitionQuery(true);
        List<Profile> profileInfo=new ArrayList<Profile>();
        String collectionLink = String.format("/dbs/%s/colls/%s", COSMOS_DB_NAME, PROFILE_TABLE_NAME);
        FeedResponse<Document> queryResults = this.documentClient.queryDocuments(collectionLink,
                "SELECT * FROM "+PROFILE_TABLE_NAME+" WHERE "+PROFILE_TABLE_NAME+".IsMainProfile=true", queryOptions);

        for (Document doc : queryResults.getQueryIterable()) {

            JSONObject objJSON=new JSONObject(doc.toJson());
            Profile profile=new Profile();
            profile.setAS2Idenitfier(objJSON.getString("AS2Identifier"));
            profile.setEemailAddress(objJSON.getString("EmailAddress"));
            profile.setAsynchronousMDNURL(objJSON.getString("AsynchronousMDNURL"));
            profile.setPrivateCertificate(objJSON.getString("PrivateCertificate"));
            profile.setIsMainProfile((objJSON.getBoolean("IsMainProfile")));
            profile.setDescription(objJSON.getString("Description"));
            profile.setOnPremHomeDirectory(objJSON.getString("OnPremHomeDirectory"));
            profile.setOnPremHomeDirectoryLinux(objJSON.getString("OnPremHomeDirectoryLinux"));
            profile.setDataFolder(objJSON.getString("DataFolder"));
            profile.setPublicCertificate(objJSON.getString("PublicCertificate"));
            profile.setCertificatePassword(objJSON.getString("CertificatePassword"));
            profileInfo.add(profile);
        }
        //System.out.println("PublicCertificate"+profileInfo.get(0).getPublicCertificate());
        //System.out.println("PrivateCertificate"+profileInfo.get(0).getPrivateCertificate());
        Constants.MainProfile=profileInfo.get(0);
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

    private void getNptyAS2DB(String strURL) throws Exception {

        String dbInfo=getCosMOSDBINFO(strURL);
        JSONArray jsonArray=  new JSONArray(dbInfo);
        Constants.APIDataInJASON=jsonArray;
        getNptyAS2DB(Constants.APIDataInJASON);
    }

    private  void getNptyAS2DB(JSONArray jsonArray) throws Exception {


        configInfo=jsonArray.getJSONObject(0);
        COSMOS_DB_NAME=configInfo.getString("CosmosDB");
        COMMANDS_TABLE_NAME=configInfo.getString("Commands");
        PROFILE_TABLE_NAME=configInfo.getString("Profile");
        NPTYAS2DEFAULTSETTINGS_TABLE_NAME=configInfo.getString("NPTYAS2DefaultSettings");
        SERVER_SETTINGS_TABLE_NAME=configInfo.getString("ServerSetting");
        LAST_UPDATED_TIME_STAMP=configInfo.getString("LastUpdatedTimeStamp");
        PARTNER_TABLE_NAME=configInfo.getString("Partner");
        PROFILE_TABLE_NAME=configInfo.getString("Profile");
        CERTIFICATE_TABLE_NAME=configInfo.getString("Certificates");
        //PARTNERSHIPS_TABLE_NAME=configInfo.getString("Partnerships");
        COMMANDPROCESSOR_TABLE_NAME=configInfo.getString("CommandProcessor");
        PROCESSOR_TABLE_NAME=configInfo.getString("Processor");
        PROPERTIES_TABLE_NAME=configInfo.getString("Properties");
        LOG_TABLE_NAME=configInfo.getString("LogTableName");
         this.documentClient = new DocumentClient(configInfo.getString("CosmosDbEndPoint"),
                configInfo.getString("CosmoDbKey"),
                new ConnectionPolicy(),
                ConsistencyLevel.Session);
        STORAGE_CONNECTION_STRING=getAzureStorageKey();
    }

    private String getCosMOSDBINFO(String strURL) throws Exception
    {
        String returnString="";
        URL url = new URL(strURL);//your url i.e fetch data from .
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
