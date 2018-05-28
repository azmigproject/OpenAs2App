package org.openas2.lib.dbUtils;

import org.joda.time.DateTime;

public class ServerModuleClasses {
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
    public boolean getIsDirectoryPollingModule() {
        return _isDirectoryPollingModule;
    }
    public void setIsDirectoryPollingModule(boolean isDirectoryPollingModule) {
        this._isDirectoryPollingModule = isDirectoryPollingModule;
    }
    public String getClassName() {
        return _className;
    }
    public void setClassName(String className) {
        this._className = className;
    }
    public String getFileName() {
        return _fileName;
    }
    public void setFileName(String _fileName) {
        this._fileName = _fileName;
    }

    private String _fileName;
    private String _className;
    private boolean _isDirectoryPollingModule;
    private String _id;
    private String _updatedBy;
    private String _createdBy;
    private DateTime _createdOn;
    private DateTime _updatedOn;
}
