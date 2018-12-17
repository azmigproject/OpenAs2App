package org.openas2.logging;
import com.microsoft.azure.storage.table.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;

public class DBLogInfo extends TableServiceEntity {
    public DBLogInfo(Level logLevel , String messageID) {
        this.partitionKey = logLevel.toString();
        this.rowKey = messageID;
    }



    public DBLogInfo() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date Now = new Date();
        this.partitionKey = sdf.format(Now);
        sdf = new SimpleDateFormat("dd HH-mm-ss.SSS");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        UUID uuid = UUID.randomUUID();
        this.rowKey =sdf.format(Now).replace(" ","").replaceAll("-","").replace(".","")+"-"+uuid.toString();
        this.source="NptyAs2 Server";
        this.isErrorMailSend=false;
        this.isnptyAS2ServerLog=true;

        }



        public String getId() {
            return id;
       }
        public void setId(String id) {
            this.id = id;
        }
        public String getSenderId() {
            return senderId;
        }
        public void setSenderId(String As2SenderId) {
            this.senderId = As2SenderId;
        }
        public String getReceiverId() {
            return receiverId;
        }
        public void setReceiverId(String AS2ReceiverId) {
            this.receiverId = AS2ReceiverId;
        }
        public String getProcessLevel() {
        return processLevel;
         }
        public void setProcessLevel(String logLevel) {
        this.processLevel = logLevel;
        }
        public String getMessageID() {
        return messageID;
         }
        public void setMessageID(String messageId) {
        this.messageID = messageId;
         }
        public String getLogMessage() {
            return logMessage;
        }
        public void setLogMessage(String message) {
            this.logMessage = message;
        }

    public String getAs2logMsgID() {
        return as2logMsgID;
    }
    public void setAs2logMsgID(String as2logMsgID) {
        this.as2logMsgID = as2logMsgID;
    }



    public String getSource() {
        return source;
    }
    public void setSource(String sources) {
        this.source = sources;
    }

    public String getFileName() {
        return fileName;
    }
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getExceptionOrErrorDetails() {
        return exceptionOrErrorDetails;
    }
    public void setExceptionOrErrorDetails(String exceptionOrErrorDetails) {
        this.exceptionOrErrorDetails = exceptionOrErrorDetails;
    }

    public boolean  getIsSuccessfull() {
        return isSuccessfull;
    }
    public void setIsSuccessfull(boolean isSuccessfull) {
        this.isSuccessfull = isSuccessfull;
    }

    public boolean  getIsnptyAS2ServerLog() {
        return isnptyAS2ServerLog;
    }
    public void setIsnptyAS2ServerLog(boolean isnptyAS2ServerLog) {
        this.isnptyAS2ServerLog = isnptyAS2ServerLog;
    }

    public boolean  getIsErrorMailSend() {
        return isErrorMailSend;
    }
    public void setIsErrorMailSend(boolean isErrorMailSend) {
        this.isErrorMailSend = isErrorMailSend;
    }

    public boolean  getIsMDNLogreceived() {
        return isMDNLogreceived;
    }
    public void setIsMDNLogreceived(boolean isMDNLogreceived) {
        this.isMDNLogreceived = isMDNLogreceived;
    }


    public String getAS2To() {
        return aS2To;
    }
    public void setAS2To(String aS2To) {
        this.aS2To = aS2To;
    }

    public String getAS2From() {
        return aS2From;
    }
    public void setAS2From(String aS2From) {
        this.aS2From = aS2From;
    }

    public String getMessageString() {
        return msgString;
    }
    public void setMessageString(String msgString) {
        this.msgString = msgString;
    }

    public boolean  getIsMsgSigned() {
        return isMsgSigned;
    }
    public void setIsMsgSigned(boolean isMsgSigned) {
        this.isMsgSigned = isMsgSigned;
    }

    public boolean  getIsMsgEncrypted() {
        return isMsgEncrypted;
    }
    public void setIsMsgEncrypted(boolean isMsgEncrypted) {
        this.isMsgEncrypted = isMsgEncrypted;
    }

    public boolean  getIsMDNRequired() {
        return isMDNRequired;
    }
    public void setIsMDNRequired(boolean isMDNRequired) {
        this.isMDNRequired = isMDNRequired;
    }

    public boolean  getIsConfiguredForMDN() {
        return isConfiguredForMDN;
    }
    public void setIsConfiguredForMDN(boolean isConfiguredForMDN) {
        this.isConfiguredForMDN = isConfiguredForMDN;
    }

    public boolean  getIsConfiguredForAsyncMDN() {
        return isConfiguredForAsyncMDN;
    }
    public void setIsConfiguredForAsyncMDN(boolean isConfiguredForAsyncMDN) {
        this.isConfiguredForAsyncMDN = isConfiguredForAsyncMDN;
    }

    public String getMDNMessageID() {
        return mdnMessageID;
    }
    public void setMDNMessageID(String mdnMessageID) {
        this.mdnMessageID = mdnMessageID;
    }

    public String getCompressionType() {
        return compressionType;
    }
    public void setCompressionType(String compressionType) {
        this.compressionType = compressionType;
    }

    public long getFileSize() {
        return fileSize;
    }
    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public long getRowId() {
        return RowId;
    }
    public void setRowId(long rowId) {
        this.RowId = rowId;
    }

    public String getContentType() {
        return contentType;
    }
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getContentDisposition() {
        return contentDisposition;
    }
    public void setContentDisposition(String contentDisposition) {
        this.contentDisposition = contentDisposition;
    }

    public String getLogMsgID()
    {
        return "ID="+id+"|As2SenderId="+senderId+"|AS2ReceiverId="+receiverId+
                "|messageText="+logMessage+"|messageId"+messageID+"|RowId"+RowId;
    }

    private String id;
    private String source;
    private String fileName;
    private long fileSize;
    private String senderId;
    private String receiverId;
    private String processLevel;
    private String logMessage;
    private String exceptionOrErrorDetails;
    private boolean isSuccessfull;
    private boolean isnptyAS2ServerLog;
    private boolean isErrorMailSend;
    private boolean isMDNLogreceived;
    private String messageID;
    private String mdnMessageID;
    private String as2logMsgID;
    private String aS2To;
    private String aS2From;
    private boolean isMsgSigned;
    private boolean isMsgEncrypted;
    private boolean isMDNRequired;
    private boolean isConfiguredForMDN;
    private boolean isConfiguredForAsyncMDN;
    private String msgString;
    private String compressionType;
    private String contentType;
    private String contentDisposition;
    private long RowId;


    }


