package org.openas2.lib.dbUtils;

public class Partnerships {

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

    private String _id;
    private String _classname;
    private String _filename;
}
