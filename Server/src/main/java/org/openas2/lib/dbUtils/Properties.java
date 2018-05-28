package org.openas2.lib.dbUtils;

import org.joda.time.DateTime;

public class Properties {

    public String getId() {
        return _id;
    }
    public void setId(String id) {
        this._id = id;
    }
   /* public String getUpdatedBy() {
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
    public String getValue() {
        return _value;
    }
    public void setValue(String value) {
        this._value = value;
    }
    public String getKey() {
        return _key;
    }
    public void setKey(String key) {
        this._key = key;
    }*/

    public String LogDateFormat() {
        return _logDateFormat;
    }
    public void LogDateFormat(String logDateFormat) {
        this._logDateFormat = logDateFormat;
    }

    public String SqlTimestampFormat() {
        return _sqlTimestampFormat;
    }
    public void SqlTimestampFormat(String sqlTimestampFormat) {
        this._sqlTimestampFormat = sqlTimestampFormat;
    }

    public String As2MessageIdFormat() {
        return _as2MessageIdFormat;
    }
    public void As2MessageIdFormat(String as2MessageIdFormat) {
        this._as2MessageIdFormat = as2MessageIdFormat;
    }

    public String BasePath() {
        return _basePath;
    }
    public void BasePath(String basePath) {
        this._basePath = basePath;
    }



    private String _id;
    /*private String _updatedBy;
    private String _createdBy;
    private DateTime _createdOn;
    private DateTime _updatedOn;
    private String _key;
    private String _value;*/

    private String _logDateFormat;
    private String _sqlTimestampFormat;
    private  String _as2MessageIdFormat;
    private  String _basePath;
}
