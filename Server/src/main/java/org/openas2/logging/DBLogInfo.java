package org.openas2.logging;
import com.microsoft.azure.storage.table.*;

public class DBLogInfo extends TableServiceEntity {
    public DBLogInfo(Level logLevel , String messageID) {
        this.partitionKey = logLevel.toString();
        this.rowKey = messageID;
    }



    public DBLogInfo() {

        }
        public String getId() {
            return id;
       }
        public void setId(String id) {
            this.id = id;
        }
        public String getAs2SenderId() {
            return As2SenderId;
        }
        public void setAs2SenderId(String As2SenderId) {
            this.As2SenderId = As2SenderId;
        }
        public String getAS2RecieverId() {
            return AS2RecieverId;
        }
        public void setAS2RecieverId(String AS2RecieverId) {
            this.AS2RecieverId = AS2RecieverId;
        }
        public Level getLogLevel() {
        return LogLevel;
         }
        public void setLogLevel(Level logLevel) {
        this.LogLevel = logLevel;
        }
        public String getMessageID() {
        return messageID;
         }
        public void setMessageID(String messageId) {
        this.messageID = messageId;
         }
        public String getMessage() {
            return messageTxt;
        }
        public void setMessage(String message) {
            this.messageTxt = message;
        }

        private String id;

        private String As2SenderId;

        private String AS2RecieverId;

        private Level LogLevel;

        private String messageTxt;

        private String messageID;

       public String getLogMsgID()
       {
           return "ID="+id+"|As2SenderId="+As2SenderId+"|AS2RecieverId="+AS2RecieverId+
                            "|messageText="+messageTxt+"|messageId"+messageID;
       }

    }

