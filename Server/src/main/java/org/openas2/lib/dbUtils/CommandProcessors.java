package org.openas2.lib.dbUtils;

import org.joda.time.DateTime;

public class CommandProcessors {


    public String getId() {
        return _id;
    }
    public void setId(String id) {
        this._id = id;
    }
    /*public String getUpdatedBy() {
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
    }*/

    public String getClassName() {
        return _className;
    }
    public void setClassName(String  className) {
        this._className = className;
    }
    public String getUserName() {
        return _username;
    }
    public void setUserName(String username) {
        this._username = username;
    }
    public String getPassword() {
        return _password;
    }
    public void setPassword(String password) {
        this._password = password;
    }
    public String getPort() {
        return _port;
    }
    public void setPort(String port) {
        this._port = port;
    }
    private String _port;
    private String _id;
    private String _className;
    private String _username;
    private String _password;
    /*private String _updatedBy;
    private String _createdBy;
    private DateTime _createdOn;
    private DateTime _updatedOn;*/

}
