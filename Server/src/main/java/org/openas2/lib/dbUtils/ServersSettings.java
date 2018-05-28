package org.openas2.lib.dbUtils;

import org.joda.time.DateTime;

public class ServersSettings {

    public String getId() {
        return _id;
    }
    public void setId(String id) {
        this._id = id;
    }
    public DateTime getCreatedOn() {
        return _createdOn;
    }
    public void setCreatedOn(DateTime createdOn) {
        this._createdOn = createdOn;
    }
    public DateTime getUpdatedOn() {
        return _updatedOn;
    }
    public void setUpdatedOn(DateTime updatedOn) {
        this._updatedOn = updatedOn;
    }
    public boolean getAllowHealthCheck() {
        return _allowHealthCheck;
    }
    public void setAllowHealthCheck(boolean allowHealthCheck) {
        this._allowHealthCheck = allowHealthCheck;
    }
    public boolean getLogEmailID() {
        return _logEmailID;
    }
    public void setLogEmailID(boolean logEmailID) {
        this._logEmailID = logEmailID;
    }
    public boolean getMailServerEnableSSl() {
        return _mailServerEnableSSl;
    }
    public void setMailServerEnableSSl(boolean mailServerEnableSSl) {
        this._mailServerEnableSSl = mailServerEnableSSl;
    }
    public int getMailServerPort() {
        return _mailServerPort;
    }
    public void setMailServerPort(int mailServerPort) {
        this._mailServerPort = mailServerPort;
    }
    public int getHealthCheckPort() {
        return _healthCheckPort;
    }
    public void setHealthCheckPort(int healthCheckPort) {
        this._healthCheckPort = healthCheckPort;
    }

    public String getMailServerSMTP() {
        return _mailServerSMTP;
    }
    public void setMailServerSMTP(String mailServerSMTP) {
        this._mailServerSMTP = mailServerSMTP;
    }
    public String getMailServerUserName() {
        return _mailServerUserName;
    }
    public void setMailServerUserName(String mailServerUserName) {
        this._mailServerUserName = mailServerUserName;
    }
    public String getMailServerPassword() {
        return _mailServerPassword;
    }
    public void setMailServerPassword(String mailServerPassword) {
        this._mailServerPassword = mailServerPassword;
    }
    public String getOnPremHomeDirectory() {
        return _onPremHomeDirectory;
    }
    public void setOnPremHomeDirectory(String onPremHomeDirectory) {
        this._onPremHomeDirectory = onPremHomeDirectory;
    }
    public String getOnPremDataFolder() {
        return _onPremDataFolder;
    }
    public void setOnPremDataFolder(String onPremDataFolder) {
        this._onPremDataFolder = onPremDataFolder;
    }
    public String getAzureCosmosDBEndPointurl() {
        return _azureCosmosDBEndPointurl;
    }
    public void setAzureCosmosDBEndPointurl(String azureCosmosDBEndPointurl) {
        this._azureCosmosDBEndPointurl = azureCosmosDBEndPointurl;
    }
    public String getQueue_InPrefix() {
        return _queue_InPrefix;
    }
    public void setQueue_InPrefix(String queue_InPrefix) {
        this._queue_InPrefix = queue_InPrefix;
    }
    public String getBlobContainerName() {
        return _blobContainerName;
    }
    public void setBlobContainerName(String blobContainerName) {
        this._blobContainerName = blobContainerName;
    }
    public String getAzureStoragekey() {
        return _azureStoragekey;
    }
    public void setAzureStoragekey(String azureStoragekey) {
        this._azureStoragekey = azureStoragekey;
    }
    public String getAzureCosmosDBPrimaryKey() {
        return _azureCosmosDBPrimaryKey;
    }
    public void setAzureCosmosDBPrimaryKey(String azureCosmosDBPrimaryKey) {
        this._azureCosmosDBPrimaryKey = azureCosmosDBPrimaryKey;
    }
    public String getQueue_ErrPrefix() {
        return _queue_ErrPrefix;
    }
    public void setQueue_ErrPrefix(String queue_ErrPrefix) {
        this._queue_ErrPrefix = queue_ErrPrefix;
    }
    public String getQueue_SentPrefix() {
        return _queue_SentPrefix;
    }
    public void setQueue_SentPrefix(String queue_SentPrefix) {
        this._queue_SentPrefix = queue_SentPrefix;
    }
    public String getLogInEmail() {
        return _logInEmail;
    }
    public void setLogInEmail(String logInEmail) {
        this._logInEmail = logInEmail;
    }
    public String getQueue_OutPrefix() {
        return _queue_OutPrefix;
    }
    public void setQueue_OutPrefix(String queue_OutPrefix) {
        this._queue_OutPrefix = queue_OutPrefix;
    }


    private String _queue_OutPrefix;
    private String _queue_ErrPrefix;
    private String _queue_SentPrefix;
    private String _id;
    private int _healthCheckPort;
    private int _mailServerPort;
    private String _logInEmail;
    private String _mailServerSMTP;
    private String _mailServerUserName;
    private String _mailServerPassword;
    private String _onPremHomeDirectory;
    private String _onPremDataFolder;
    private String _azureCosmosDBEndPointurl;
    private String _azureCosmosDBPrimaryKey;
    private String _azureStoragekey;
    private String _blobContainerName;
    private String _queue_InPrefix;

    private DateTime _createdOn;
    private DateTime _updatedOn;
    private boolean _mailServerEnableSSl;
    private boolean _logEmailID;
    private boolean _allowHealthCheck;
}
