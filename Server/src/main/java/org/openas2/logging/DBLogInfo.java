package org.openas2.logging;
import com.microsoft.azure.storage.table.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class DBLogInfo extends TableServiceEntity {
    public DBLogInfo(Level logLevel , String messageID) {
        this.partitionKey = logLevel.toString();
        this.rowKey = messageID;
    }



    public DBLogInfo() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM");
        Date Now = new Date();
        this.partitionKey = sdf.format(Now);
        sdf = new SimpleDateFormat("dd HH-mm-ss.SSS");
        UUID uuid = UUID.randomUUID();
        this.rowKey =sdf.format(Now)+"-"+uuid.toString();
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
        public String getRecieverId() {
            return recieverId;
        }
        public void setRecieverId(String AS2RecieverId) {
            this.recieverId = AS2RecieverId;
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


    public String getMDNMessageID() {
        return mdnMessageID;
    }
    public void setMDNMessageID(String mdnMessageID) {
        this.mdnMessageID = mdnMessageID;
    }

    public String getLogMsgID()
    {
        return "ID="+id+"|As2SenderId="+senderId+"|AS2RecieverId="+recieverId+
                "|messageText="+logMessage+"|messageId"+messageID;
    }

    private String id;
    private String source;
    private String fileName;
    private String senderId;
    private String recieverId;
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


    }

