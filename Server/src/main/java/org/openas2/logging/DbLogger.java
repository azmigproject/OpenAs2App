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
public class DbLogger extends BaseLogger {


    private static final String AZURE_TABLE_NAME = "DBLog";
    private static final String  STORAGE_CONNECTION_STRING = "UseDevelopmentStorage=true";
    private CloudTableClient client;




    public void init(Session session, Map<String, String> parameters) throws OpenAS2Exception {
        super.init(session, parameters);
        // check if log file can be created
        try {
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
        }

    }

    public void doLog(Level level, String msgText, Message as2Msg) {

        try {
            DBLogInfo objInfo=GetDBLogInfo(level,msgText,as2Msg);
            AddLogInTable(objInfo);
        }
        catch (Exception e)
        {
            System.out.println("Error in Document ");
            e.printStackTrace();
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

    private void getLogDB() throws Exception {


            CloudStorageAccount storageAccount =
                    CloudStorageAccount.parse(STORAGE_CONNECTION_STRING);
            this.client = storageAccount.createCloudTableClient();

            // Create the table if it doesn't exist.

            CloudTable cloudTable = this.client.getTableReference(AZURE_TABLE_NAME);
            cloudTable.createIfNotExists();


    }




    private  DBLogInfo GetDBLogInfo(Level level, String msgText, Message as2Msg)
    {
       //ToDo Working on changes
        DBLogInfo objLog=new DBLogInfo();
        objLog.setId(UUID.randomUUID().toString());
        if(as2Msg!=null) {
            objLog.setAS2RecieverId(as2Msg.getPartnership().getSenderIDs().entrySet().iterator().next().getValue().toString());
        }
        return objLog;
    }

    private  DBLogInfo GetDBLogInfo(Throwable t, boolean terminated)
    {
        DBLogInfo objLog=new DBLogInfo();
        return objLog;
    }


    private void  AddLogInTable   (DBLogInfo  objLog)
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


