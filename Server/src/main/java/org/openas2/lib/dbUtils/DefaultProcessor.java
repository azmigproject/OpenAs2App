package org.openas2.lib.dbUtils;

import org.joda.time.DateTime;

public class DefaultProcessor {

    public String getId() {
        return _id;
    }
    public void setId(String id) {
        this._id = id;
    }
    public String getUpdatedBy() {
        return _updatedBy;
    }
    public void setUpdatedBy(String updatedBy) {
        this._updatedBy = updatedBy;
    }
    public String getCreatedBy() {
        return _createdBy;
    }
    public void setCreatedBy(String createdBy) {
        this._createdBy = createdBy;
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
    public String getClassNameId() {
        return _classNameId;
    }
    public void setClassNameId(String  classNameId) {
        this._classNameId = classNameId;
    }
    public String getPendingMDN() {
        return _pendingMDN;
    }
    public void setPendingMDN(String pendingMDN) {
        this._pendingMDN = pendingMDN;
    }
    public String getPendingMDNInfo() {
        return _pendingMDNInfo;
    }
    public void setPendingMDNInfo(String pendingMDNInfo) {
        this._pendingMDNInfo = pendingMDNInfo;
    }

    private String _id;
    private String _classNameId;
    private String _pendingMDNInfo;
    private String _pendingMDN;
    private String _updatedBy;
    private String _createdBy;
    private DateTime _createdOn;
    private DateTime _updatedOn;
}
