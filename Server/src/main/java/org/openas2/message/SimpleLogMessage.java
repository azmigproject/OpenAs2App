package org.openas2.message;


import org.openas2.partner.Partnership;

public class SimpleLogMessage {

    private String fileName="";
    private String messageId="";
    private String senderId="";
    private String receiverId="";
    private String logMessage="";


    public String getLogMessage()
    {
        return logMessage;
    }

    public void setLogMessage(String logMsg)
    {
        this.logMessage = logMsg;
    }

    public String getFileName()
    {
        return fileName;
    }

    public void setFileName(String file)
    {
        this.fileName = file;
    }

    public String getMessageId()
    {
        return messageId;
    }

    public void setMessageId(String strId)
    {
        this.messageId = strId;
    }

    public String getSenderId()
    {
        return senderId;
    }

    public void setSenderId(String strId)
    {
        this.senderId = strId;
    }

    public String getReceiverId()
    {
        return receiverId;
    }

    public void setReceiverId(String strId)
    {
        this.receiverId = strId;
    }

    public  Message getMessageObject()
    {
       AS2Message msg=new AS2Message();
       msg.setLogMsg(this.logMessage);
       msg.setPayloadFilename(this.fileName);
        Partnership pt=new Partnership();
        pt.setSenderID("",this.senderId);
        pt.setReceiverID("",this.receiverId);
       msg.setPartnership(pt);
       msg.setHeader("",this.senderId);
        msg.setHeader("",this.receiverId);
        msg.setMessageID(this.messageId);
       return  msg;
    }

}
