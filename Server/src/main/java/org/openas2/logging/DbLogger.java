package org.openas2.logging;

import org.openas2.OpenAS2Exception;
import org.openas2.Session;
import org.openas2.message.Message;

import java.util.Map;
import java.util.UUID;

// Include the following imports to use table APIs
import com.microsoft.azure.storage.*;
import com.microsoft.azure.storage.table.*;
import com.microsoft.azure.storage.table.TableQuery.*;
import org.openas2.partner.AS2Partnership;
import org.openas2.util.AzureUtil;

public class DbLogger extends BaseLogger {


    private  String AZURE_TABLE_NAME = "DBLog";
    private  String  STORAGE_CONNECTION_STRING = "UseDevelopmentStorage=true";
    private CloudTableClient client;


    public DbLogger(String tableName,String conString) throws Exception {

        setLogTableInfo(tableName, conString);

    }


    public void init(Session session, Map<String, String> parameters) throws OpenAS2Exception {
        super.init(session, parameters);
        // check if log file can be created
        /*try {
            getLogDB();
        }
        catch (TableServiceException e)
        {
            System.out.println("Error in Document ");
            e.printStackTrace();
        }

        catch (Exception e)
        {
            System.out.println("Error in Document ");
            e.printStackTrace();
        }*/

    }

    public void doLog(Level level, String msgText, Message as2Msg) {
        if (level.getName() == "error" || level.getName() == "warning" || as2Msg != null) {
            try {
                DBLogInfo objInfo = GetDBLogInfo(level, msgText, as2Msg);
                AddLogInTable(objInfo);
            } catch (Exception e) {
                System.out.println("Error in Document ");
                e.printStackTrace();
            }
        }
    }

    public void doLog(Level level, String msgText, DBLogInfo as2Msg) {
        if (level.getName() == "error" || level.getName() == "warning" || as2Msg != null) {
            try {

                AddLogInTable(as2Msg);
            } catch (Exception e) {
                System.out.println("Error in Document ");
                e.printStackTrace();
            }
        }
    }

    protected String getShowDefaults() {
        return VALUE_SHOW_ALL;
    }

    protected void doLog(Throwable t, boolean terminated) {
        try {
            DBLogInfo objInfo=GetDBLogInfo(t,terminated);
            AddLogInTable(objInfo);
        }
        catch (Exception e)
        {
            System.out.println("Error in Document ");
            e.printStackTrace();
        }
    }



    public void setLogTableInfo(String tableName,String conString) throws Exception {


        AZURE_TABLE_NAME=tableName;
        STORAGE_CONNECTION_STRING=conString;
            CloudStorageAccount storageAccount =
                    CloudStorageAccount.parse(STORAGE_CONNECTION_STRING);
            this.client = storageAccount.createCloudTableClient();

            // Create the table if it doesn't exist.

          //  CloudTable cloudTable = this.client.getTableReference(AZURE_TABLE_NAME);
            //cloudTable.createIfNotExists();


    }




    private  DBLogInfo GetDBLogInfo(Level level, String msgText, Message as2Msg)
    {
       //ToDo Working on changes
        DBLogInfo objLog=new DBLogInfo();
        objLog.setId(UUID.randomUUID().toString());
        objLog.setProcessLevel(level.getName());
        objLog.setIsnptyAS2ServerLog(true);
        objLog.setIsSuccessfull(true);
        objLog.setIsErrorMailSend(false);
        if(as2Msg!=null) {
            objLog.setReceiverId(as2Msg.getPartnership().getReceiverID(AS2Partnership.PID_AS2));
            objLog.setSenderId(as2Msg.getPartnership().getSenderID(AS2Partnership.PID_AS2));
            objLog.setFileName(as2Msg.getPayloadFilename());
            objLog.setMessageID(as2Msg.getMessageID());
            objLog.setAs2logMsgID(as2Msg.getLogMsgID());
            objLog.setMDNMessageID(as2Msg.getMDN().getMessageID());
            objLog.setAS2To(as2Msg.getHeader("As2-To"));
            objLog.setAS2From(as2Msg.getHeader("As2-From"));
            objLog.setIsConfiguredForMDN(as2Msg.isConfiguredForMDN());
            objLog.setIsConfiguredForAsyncMDN(as2Msg.isConfiguredForAsynchMDN());
            objLog.setIsMDNRequired(as2Msg.isRequestingMDN());
            objLog.setMessageString(as2Msg.toString());
            objLog.setCompressionType(as2Msg.getCompressionType());
            objLog.setContentDisposition(as2Msg.getContentDisposition());
            objLog.setContentType(as2Msg.getContentType());
            objLog.setFileSize(Long.getLong(as2Msg.getHeader("Content-Length"))); //TO DO ADD OPTION FOR FILE SIZE
        }

            objLog.setLogMessage(msgText);


        return objLog;
    }

    private  DBLogInfo GetDBLogInfo(Throwable t, boolean terminated)
    {
        DBLogInfo objLog=new DBLogInfo();
        objLog.setId(UUID.randomUUID().toString());
        objLog.setProcessLevel(Level.ERROR.getName());
        objLog.setIsnptyAS2ServerLog(true);
        objLog.setExceptionOrErrorDetails(t.getMessage()+t.getStackTrace());
        objLog.setIsSuccessfull(false);
        return objLog;
    }


    private void  AddLogInTable(DBLogInfo  objLog)
    {
        try {
            // Create an operation to add the new customer to the people table.
            // Create a cloud table object for the table.
            CloudTable cloudTable = this.client.getTableReference(AZURE_TABLE_NAME);

            TableOperation insertLog = TableOperation.insertOrReplace(objLog);

            // Submit the operation to the table service.
            cloudTable.execute(insertLog);
        }
        catch (Exception e)
        {
            System.out.println("Error in Document ");
            e.printStackTrace();
        }

    }



}


