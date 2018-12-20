package org.openas2.logging;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.joda.time.DateTime;
import org.openas2.OpenAS2Exception;
import org.openas2.Session;
import org.openas2.message.Message;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.jar.JarFile;

// Include the following imports to use table APIs
import com.microsoft.azure.storage.*;
import com.microsoft.azure.storage.table.*;
import com.microsoft.azure.storage.table.TableQuery.*;
import org.openas2.partner.AS2Partnership;
import org.openas2.util.AzureUtil;

public class DbLogger extends BaseLogger {


    private  String AZURE_TABLE_NAME = "DBLog";
    private  String AZURE_COUNTER_TABLE_NAME = "nptyAS2logCount";
    private  String  STORAGE_CONNECTION_STRING = "UseDevelopmentStorage=true";
    private CloudTableClient client;


    public DbLogger(String tableName, String counterTableName, String conString) throws Exception {

        setLogTableInfo(tableName,counterTableName, conString);

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



    public void setLogTableInfo(String tableName,String counterTableName,String conString) throws Exception {


        AZURE_TABLE_NAME=tableName;
        AZURE_COUNTER_TABLE_NAME=counterTableName;
        STORAGE_CONNECTION_STRING=conString;
            CloudStorageAccount storageAccount =
                    CloudStorageAccount.parse(STORAGE_CONNECTION_STRING);
            this.client = storageAccount.createCloudTableClient();

            // Create the table if it doesn't exist.

          //  CloudTable cloudTable = this.client.getTableReference(AZURE_TABLE_NAME);
            //cloudTable.createIfNotExists();


    }




    private  DBLogInfo GetDBLogInfo(Level level, String msgText, Message as2Msg) throws Exception
    {
       //ToDo Working on changes
        DBLogInfo objLog=new DBLogInfo();
        objLog.setId(UUID.randomUUID().toString());
        objLog.setProcessLevel(level.getName());
        objLog.setIsnptyAS2ServerLog(true);
        objLog.setIsSuccessfull(true);
        objLog.setIsErrorMailSend(false);
        if(as2Msg!=null) {
            if(as2Msg.getPartnership()!=null) {
                objLog.setReceiverId(as2Msg.getPartnership().getReceiverID(AS2Partnership.PID_AS2));
                objLog.setSenderId(as2Msg.getPartnership().getSenderID(AS2Partnership.PID_AS2));
            }
            objLog.setFileName(as2Msg.getPayloadFilename());
            objLog.setMessageID(as2Msg.getMessageID());
            objLog.setAs2logMsgID(as2Msg.getLogMsgID());
            objLog.setMDNMessageID("");
            if(as2Msg.getMDN()!=null) {
                objLog.setMDNMessageID(as2Msg.getMDN().getMessageID());
            }
            objLog.setAS2To(as2Msg.getHeader("As2-To"));
            objLog.setAS2From(as2Msg.getHeader("As2-From"));
            objLog.setIsMsgEncrypted(as2Msg.getPartnership().getAttribute("encrypt")!=null);
            objLog.setIsMsgSigned(as2Msg.getPartnership().getAttribute("sign")!=null);
            objLog.setIsConfiguredForMDN(as2Msg.isConfiguredForMDN());
            objLog.setIsConfiguredForAsyncMDN(as2Msg.isConfiguredForAsynchMDN());
            objLog.setIsMDNRequired(as2Msg.isRequestingMDN());
            objLog.setMessageString(as2Msg.toString());
            objLog.setCompressionType(as2Msg.getCompressionType());
            objLog.setContentDisposition(as2Msg.getContentDisposition());
            objLog.setContentType(as2Msg.getContentType());
            objLog.setFileSize(0);
            if(as2Msg.getData()!=null) {
                objLog.setFileSize(as2Msg.getData().getSize());
            }//TO DO ADD OPTION FOR FILE SIZE
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
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        objLog.setExceptionOrErrorDetails(t.getMessage()+"|Details:"+sw.toString());
        objLog.setIsSuccessfull(false);
        return objLog;
    }

    private TableQuery<DBLogCounterInfo> GetLogCountTableQuery(String PartitionKey)
    {
        String Query = String.format("PartitionKey eq '{0}'", PartitionKey);
        TableQuery<DBLogCounterInfo> DataTableQuery = new TableQuery<DBLogCounterInfo>();
        DataTableQuery.where(Query);
        return DataTableQuery.where(Query);
    }

    private String  GetCountsFromCounterTable(CloudTable cloudCounterTable,String PartitionKey)
    {
        long MaxCount=0;
        long RowCount=0;
        long CurrCount=0;
        long PrevMaxCount=0;
        String RowKey="";


        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String PrevPartitionKey=  sdf.format(DateTime.now().minusDays(1).toDate());

        // Create a filter condition where the partition key is "Smith".
        String partitionFilter = TableQuery.generateFilterCondition(
                "PartitionKey",
                QueryComparisons.NOT_EQUAL,
                "null");

        // Specify a partition query, using "Smith" as the partition key filter.
        TableQuery<DBLogCounterInfo> partitionQuery =
                TableQuery.from(DBLogCounterInfo.class)
                        .where(partitionFilter);




                try {
                    for (DBLogCounterInfo dbLogCounterinfo : cloudCounterTable.execute(partitionQuery)) {

                        RowCount += dbLogCounterinfo.getCount();
                        if (dbLogCounterinfo.getPartitionKey().compareTo(PartitionKey.trim())==0) {
                            CurrCount = dbLogCounterinfo.getCount();
                            MaxCount = dbLogCounterinfo.getAvailRecordCount();
                            RowKey = dbLogCounterinfo.getRowKey();

                        }
                        if (dbLogCounterinfo.getPartitionKey().compareTo( PrevPartitionKey.trim())==0) {

                            PrevMaxCount = dbLogCounterinfo.getAvailRecordCount();

                        }
                    }
                }
                catch (Exception ex)
                {
                    MaxCount = -1;
                    RowCount = -1;
                    CurrCount=-1;
                    System.out.println("Process invalid data handle");
                }


            if(MaxCount==0)
            {
                MaxCount=PrevMaxCount;
            }

        return MaxCount+"|"+RowCount+"|"+CurrCount+"|"+RowKey;

    }

    private void  AddLogInTable(DBLogInfo  objLog) {
        try {
            // Create an operation to add the new customer to the people table.
            // Create a cloud table object for the table.
            CloudTable cloudTable = this.client.getTableReference(AZURE_TABLE_NAME);
            CloudTable cloudCounterTable = this.client.getTableReference(AZURE_COUNTER_TABLE_NAME);



                    //Get the RowId

                    if (cloudTable != null && cloudCounterTable != null) {
                        synchronized (cloudTable) {
                            synchronized (cloudCounterTable) {
                                if (objLog != null) {
                                    if (objLog.getExceptionOrErrorDetails() != null && objLog.getExceptionOrErrorDetails().length() > 1000) {
                                        objLog.setExceptionOrErrorDetails(objLog.getExceptionOrErrorDetails().trim().substring(0, 999));
                                    }

                                    String Counters = GetCountsFromCounterTable(cloudCounterTable, objLog.getPartitionKey());
                                    System.out.println(Counters);
                                    String[] intVals = Counters.split("\\|");  //MaxCount+"|"+RowCount+"|"+CurrCount+"|"+RowKey;

                                    long AllRowCounter = Long.parseLong(intVals[0].trim()); //MaxCount
                                    long RowCount = Long.parseLong(intVals[1].trim()); //RowCount
                                    long Count = Long.parseLong(intVals[2].trim());//CurrCount
                                    String RowKey = intVals[3]; //RowKey

                                    if (AllRowCounter > -1 && RowCount > -1) {
                                        AllRowCounter = AllRowCounter + 1;
                                        Count = Count + 1;
                                        //RowCount = RowCount + 1;
                                        objLog.setRowId(Count);
                                        TableOperation insertLog = TableOperation.insertOrReplace(objLog);

                                        DBLogCounterInfo LogCountertable = new DBLogCounterInfo();
                                        LogCountertable.setPartitionKey(objLog.getPartitionKey());
                                        LogCountertable.setCount(Count);
                                        LogCountertable.setRowKey(RowKey);
                                        LogCountertable.setAvailRecordCount(AllRowCounter);
                                        // Submit the operation to the table service.
                                        cloudTable.execute(insertLog);

                                        TableOperation insertLogCounter = TableOperation.insertOrReplace(LogCountertable);

                                        cloudCounterTable.execute(insertLogCounter);


                                    } else {
                                        System.out.println("Unable to log message as not able to get Data from Logcounter table. ObjLog Info=" + objLog.getLogMessage());

                                    }
                                } else {
                                    System.out.println("Unable to log message as DBLogInfo object null");
                                }
                            }
                        }
                    } else {
                        System.out.println("Unable to log message as cloudTable object null");

                    }


        } catch (Exception e) {
            System.out.println("Error in Document ");
            e.printStackTrace();
        }

    }

}


