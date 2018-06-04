package org.openas2.lib.dbUtils;

import org.joda.time.DateTime;

public class  module {

    public String getProtocol() {
        return _protocol;
    }
    public void setProtocol(String protocol) {
        this._protocol = protocol;
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
    }*/
    public String getClassName() {
        return _className;
    }
    public void setClassName(String className) {
        this._className = className;
    }
    public String getSendFileName() {
        return _sendfilename;
    }
    public void setSendFileName(String sendfilename) {
        this._sendfilename = sendfilename;
    }
    public String getFileName() {
        return _filename;
    }
    public void setFileName(String filename) {
        this._filename = filename;
    }
    public String getHeader() {
        return _header;
    }
    public void setHeader(String header) {
        this._header = header;
    }

    public String getTempDir() {
        return _tempDir;
    }
    public void setTempDir(String tempDir) {
        this._tempDir = tempDir;
    }

    public String getOutboxDir() {
        return _outboxDir;
    }
    public void setOutboxDir(String outboxDir) {
        this._outboxDir = outboxDir;
    }
    public String getErrorDir() {
        return _errorDir;
    }
    public void setErrorDir(String errorDir) {
        this._errorDir = errorDir;
    }
    public String getErrorFormat() {
        return _errorFormat;
    }
    public void setErrorFormat(String errorFormat) {
        this._errorFormat = errorFormat;
    }

    public String getResendDir() {
        return _resendDir;
    }
    public void setResendDir(String resendDir) {
        this._resendDir = resendDir;
    }

    public int getRetries() {
        return _retries;
    }
    public void setRetries(int retries) {
        this._retries = retries;
    }

    public int getResendDelay() {
        return _resendDelay;
    }
    public void setResendDelay(int resendDelay) {
        this._resendDelay = resendDelay;
    }
    public String getPort() {
        return _port;
    }
    public void setPort(String port) {
        this._port = port;
    }
    public String getDelimiters() {
        return _delimiters;
    }
    public void setDelimiters(String delimiters) {
        this._delimiters = delimiters;
    }

    public String getMimetype() {
        return _mimetype;
    }
    public void setMimetype(String mimetype) {
        this._mimetype = mimetype;
    }
    public String getFormat() {
        return _format;
    }
    public void setFormat(String format) {
        this._format = format;
    }
    public int getInterval() {
        return _interval;
    }
    public void setInterval(int interval) {
        this._interval = interval;
    }

    public String getDefaults() {
        return _defaults;
    }
    public void setDefaults(String defaults) {
        this._defaults = _defaults;
    }

    public String getQueueName() {
        return _queuename;
    }
    public void setQueueName(String queuename) {
        this._queuename = queuename;
    }

    private int _retries;
    private String _port;
    private int _interval;
    private int _resendDelay;
    private String _protocol;
    private String _className;
    private String _sendfilename;
    private String _filename;
    private String _header;
    private String _tempDir;
    private String _errorDir;
    private String _outboxDir;
    private String _errorFormat;
    private String _resendDir;
    private String _delimiters;
    private String _mimetype;
    private String _format;
    private String _defaults;
    private String _queuename;
    /*private String _updatedBy;
    private String _createdBy;
    private DateTime _createdOn;
    private DateTime _updatedOn;*/


}
