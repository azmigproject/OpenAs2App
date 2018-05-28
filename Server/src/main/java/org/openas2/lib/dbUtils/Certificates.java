package org.openas2.lib.dbUtils;

public class Certificates {

    public String getId() {
        return _id;
    }
    public void setId(String id) {
        this._id = id;
    }

    public String getClassName() {
        return _classname;
    }
    public void setClassName(String classname) {
        this._classname = classname;
    }

    public String getFileName() {
        return _filename;
    }
    public void setFileName(String filename) {
        this._filename = filename;
    }

    public String getPassword() {
        return _password;
    }
    public void setPassword(String password) {
        this._password = password;
    }

    public int getInterval() {
        return _interval;
    }
    public void setInterval(int interval) {
        this._interval = interval;
    }



    private String _id;
    private String _classname;
    private String _filename;
    private  String _password;
    private int _interval;
}
