package org.openas2.lib.dbUtils;

import org.joda.time.DateTime;

public class partner {

    public String getId() {
        return _id;
    }
    public void setId(String id) {
        this._id = id;
    }
    public String getPartnerName() {
        return _partnerName;
    }
    public void setPartnerName(String partnerName) {
        this._partnerName = partnerName;
    }
    public String getAS2Identifier() {
        return _as2Identifier;
    }
    public void setAS2Identifier(String as2Identifier) {
        this._as2Identifier = as2Identifier;
    }

    public String getPartnerUrl() {
        return _partnerUrl;
    }
    public void setPartnerUrl(String partnerUrl) {
        this._partnerUrl = partnerUrl;
    }
    public boolean getEnableAutomation() {
        return _enableAutomation;
    }
    public void setEnableAutomation(boolean enableAutomation) {
        this._enableAutomation = enableAutomation;
    }
    public boolean getIsMDNRequested() {
        return _isMDNRequested;
    }
    public void setIsMDNRequested(boolean isMDNRequested) {
        this._isMDNRequested = isMDNRequested;
    }
    public boolean getEncryptOutgoingMessage() {
        return _encryptOutgoingMessage;
    }
    public void setEncryptOutgoingMessage(boolean encryptOutgoingMessage) {
        this._encryptOutgoingMessage = encryptOutgoingMessage;
    }
    public boolean getIncomingMessageRequireSignature() {
        return _incomingMessageRequireSignature;
    }
    public void setIncomingMessageRequireSignature(boolean incomingMessageRequireSignature) {
        this._incomingMessageRequireSignature = _incomingMessageRequireSignature;
    }
    public boolean getIncomingMessageRequireEncryption() {
        return _incomingMessageRequireEncryption;
    }
    public void setIncomingMessageRequireEncryption(boolean incomingMessageRequireEncryption) {
        this._incomingMessageRequireEncryption = incomingMessageRequireEncryption;
    }

    public boolean getSignOutgoingMessage() {
        return _signOutgoingMessage;
    }
    public void setSignOutgoingMessage(boolean signOutgoingMessage) {
        this._signOutgoingMessage = signOutgoingMessage;
    }
    public boolean getISMDNSigned() {
        return _isMDNSigned;
    }
    public void setISMDNSigned(boolean isMDNSigned) {
        this._isMDNSigned = isMDNSigned;
    }
    public int getConnectionTimeOutInSec() {
        return _connectionTimeOutInSec;
    }
    public void setConnectionTimeOutInSec(int connectionTimeOutInSec) {
        this._connectionTimeOutInSec = connectionTimeOutInSec;
    }
    public boolean getIsSyncronous() {
        return _isSyncronous;
    }
    public void setIsSyncronous(boolean isSyncronous) {
        this._isSyncronous = isSyncronous;
    }
    public int getRetryInterval() {
        return _retryInterval;
    }
    public void setRetryInterval(int retryInterval) {
        this._retryInterval = retryInterval;
    }
    public int getResendInterval() {
        return _resendInterval;
    }
    public void setResendInterval(int resendInterval) {
        this._resendInterval = resendInterval;
    }
    public int getMaxAttempts() {
        return _maxAttempts;
    }
    public void setMaxAttempts(int maxAttempts) {
        this._maxAttempts = maxAttempts;
    }
    public boolean getSendFileNameInContentType() {
        return _sendFileNameInContentType;
    }
    public void setSendFileNameInContentType(boolean sendFileNameInContentType) {
        this._sendFileNameInContentType = sendFileNameInContentType;
    }
    public boolean getIsFolderCreated() {
        return _isFolderCreated;
    }
    public void setIsFolderCreated(boolean isFolderCreated) {
        this._isFolderCreated = isFolderCreated;
    }
    public String getOnPremErrDirName() {
        return _onPremErrDirName;
    }
    public void setOnPremErrDirName(String onPremErrDirName) {
        this._onPremErrDirName = onPremErrDirName;
    }
    public boolean getIsMessageCompressed() {
        return _isMessageCompressed;
    }
    public void setIsMessageCompressed(boolean isMessageCompressed) {
        this._isMessageCompressed = isMessageCompressed;
    }
    public String getBlobFoldername() {
        return _blobFoldername;
    }
    public void setBlobFoldername(String blobFoldername) {
        this._blobFoldername = blobFoldername;
    }
    public String getEncryptionAlgorithm() {
        return _encryptionAlgorithm;
    }
    public void setEncryptionAlgorithm(String encryptionAlgorithm) {
        this._encryptionAlgorithm = encryptionAlgorithm;
    }
    public String getSignatureAlgorithm() {
        return _signatureAlgorithm;
    }
    public void setSignatureAlgorithm(String signatureAlgorithm) {
        this._signatureAlgorithm = signatureAlgorithm;
    }
    public String getPublicCertificate() {
        return _publicCertificate;
    }
    public void setPublicCertificate(String publicCertificate) {
        this._publicCertificate = publicCertificate;
    }
    public String getEmailAddress() {
        return _emailAddress;
    }
    public void setEmailAddress(String emailAddress) {
        this._emailAddress = emailAddress;
    }
    public String getInErrorQueue() {
        return _inErrorQueue;
    }
    public void setInErrorQueue(String inErrorQueue) {
        this._inErrorQueue = inErrorQueue;
    }
    public String getSentQueue() {
        return _sentQueue;
    }
    public void setSentQueue(String sentQueue) {
        this._sentQueue = sentQueue;
    }
    public String getOutgoingQueue() {
        return _outgoingQueue;
    }
    public void setOutgoingQueue(String outgoingQueue) {
        this._outgoingQueue = outgoingQueue;
    }
    public String getIncomingQueue() {
        return _incomingQueue;
    }
    public void setIncomingQueue(String incomingQueue) {
        this._incomingQueue = incomingQueue;
    }
    public String getCreatedBy() {
        return _createdBy;
    }
    public void setCreatedBy(String createdBy) {
        this._createdBy = createdBy;
    }
    public String getOnPremSentDirName() {
        return _onPremSentDirName;
    }
    public void setOnPremSentDirName(String onPremSentDirName) {
        this._onPremSentDirName = onPremSentDirName;
    }
    public String getOnPremOutErrorDirName() {
        return _onPremOutErrorDirName;
    }
    public void setOnPremOutErrorDirName(String onPremOutErrorDirName) {
        this._onPremOutErrorDirName = onPremOutErrorDirName;
    }
    public String getOnPremInErrorDirName() {
        return _onPremInErrorDirName;
    }
    public void setOnPremInErrorDirName(String onPremInErrorDirName) {
        this._onPremInErrorDirName = onPremInErrorDirName;
    }
    public String getOnPremOutgoingDirName() {
        return _onPremOutgoingDirName;
    }
    public void setOnPremOutgoingDirName(String onPremOutgoingDirName) {
        this._onPremOutgoingDirName = onPremOutgoingDirName;
    }
    public String getOnPremIncomingDirName() {
        return _onPremIncomingDirName;
    }
    public void setOnPremIncomingDirName(String onPremIncomingDirName) {
        this._onPremIncomingDirName = onPremIncomingDirName;
    }
    public String getUpdatedBy() {
        return _updatedBy;
    }
    public void setUpdatedBy(String updatedBy) {
        this._updatedBy = updatedBy;
    }
    /*public String getOutErrorQueue() {
        return _outErrorQueue;
    }
    public void setOutErrorQueue(String outErrorQueue) {
        this._outErrorQueue = outErrorQueue;
    }*/
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

    public boolean getSSLEnabledProtocolsSSLv2() {
        return _sslEnabledProtocolsSSLv2;
    }
    public void SetSSLEnabledProtocolsSSLv2(boolean sslEnabledProtocolsSSLv2) {
        this._sslEnabledProtocolsSSLv2 = sslEnabledProtocolsSSLv2;
    }

    public boolean getSSLEnabledProtocolsSSLv3() {
        return _sslEnabledProtocolsSSLv3;
    }
    public void SetSSLEnabledProtocolsSSLv3(boolean sslEnabledProtocolsSSLv3) {
        this._sslEnabledProtocolsSSLv3 = sslEnabledProtocolsSSLv3;
    }

    public boolean getSSLEnabledProtocolsTLSv1() {
        return _sslEnabledProtocolsTLSv1;
    }
    public void SetSSLEnabledProtocolsTLSv1(boolean sslEnabledProtocolsTLSv1) {
        this._sslEnabledProtocolsTLSv1 = sslEnabledProtocolsTLSv1;
    }

    public boolean getSSLEnabledProtocolsTLSv11() {
        return _sslEnabledProtocolsTLSv11;
    }
    public void SetSSLEnabledProtocolsTLSv11(boolean sslEnabledProtocolsTLSv11) {
        this._sslEnabledProtocolsTLSv11 = sslEnabledProtocolsTLSv11;
    }

    public boolean getSSLEnabledProtocolsTLSv12() {
        return _sslEnabledProtocolsTLSv12;
    }
    public void SetSSLEnabledProtocolsTLSv12(boolean sslEnabledProtocolsTLSv12) {
        this._sslEnabledProtocolsTLSv12 = sslEnabledProtocolsTLSv12;
    }


    //private String _outErrorQueue;
    private DateTime _createdOn;
    private DateTime _updatedOn;
    private String _updatedBy;
    private String _onPremIncomingDirName;
    private String _onPremOutgoingDirName;
    private String _onPremInErrorDirName;
    private String _onPremOutErrorDirName;
    private String _onPremSentDirName;
    private String _createdBy;
    private String _incomingQueue;
    private String _outgoingQueue;
    private String _sentQueue;
    private String _inErrorQueue;
    private String _emailAddress;
    private String _publicCertificate;
    private String _signatureAlgorithm;
    private String _encryptionAlgorithm;
    private String _blobFoldername;
    private boolean _enableAutomation;
    private boolean _isSyncronous;
    private boolean _sendFileNameInContentType;
    private boolean _isFolderCreated;
    private boolean _isMessageCompressed;
    private String _onPremErrDirName;
    private int _retryInterval;
    private int _resendInterval;
    private int _maxAttempts;
    private boolean _isMDNSigned;
    private boolean _incomingMessageRequireEncryption;
    private boolean _incomingMessageRequireSignature;
    private int _connectionTimeOutInSec;
    private boolean _signOutgoingMessage;
    private boolean _encryptOutgoingMessage;
    private boolean _isMDNRequested;
    private boolean _sslEnabledProtocolsSSLv2;
    private boolean _sslEnabledProtocolsSSLv3;
    private boolean _sslEnabledProtocolsTLSv1;
    private boolean _sslEnabledProtocolsTLSv11;
    private boolean _sslEnabledProtocolsTLSv12;
    private String _partnerUrl;
    private String _as2Identifier;
    private String _partnerName;
    private String _id;


}
