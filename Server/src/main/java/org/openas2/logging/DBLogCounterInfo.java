package org.openas2.logging;

import com.microsoft.azure.storage.table.TableServiceEntity;

public class DBLogCounterInfo extends TableServiceEntity {

    private long AvailRecordCount;
    private long Count;


    public long getCount() {
        return Count;
    }
    public void setCount(long rowCount) {
        this.Count = rowCount;
    }


    public long getAvailRecordCount() {
        return AvailRecordCount;
    }
    public void setAvailRecordCount(long totalRowCount) {
        this.AvailRecordCount = totalRowCount;
    }


}
