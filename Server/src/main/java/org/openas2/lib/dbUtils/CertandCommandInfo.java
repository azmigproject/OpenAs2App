package org.openas2.lib.dbUtils;

import org.joda.time.DateTime;

public class CertandCommandInfo {
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
    public String getFileName() {
        return _fileName;
    }
    public void setFileName(String fileName) {
        this._fileName = fileName;
    }
    public String getPassword() {
        return _password;
    }
    public void setPassword(String password) {
        this._password = password;
    }
    public String getNodeName() {
        return _nodeName;
    }
    public void setNodeName(String nodeName) {
        this._nodeName = nodeName;
    }
    public int getInterval() {
        return _interval;
    }
    public void setInterval(int interval) {
        this._interval = interval;
    }
    private int _interval;
    private String _id;
    private String _classNameId;
    private String _fileName;
    private String _password;
    private String _nodeName;
    private String _updatedBy;
    private String _createdBy;
    private DateTime _createdOn;
    private DateTime _updatedOn;
}
