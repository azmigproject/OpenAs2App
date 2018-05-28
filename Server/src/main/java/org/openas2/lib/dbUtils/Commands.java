package org.openas2.lib.dbUtils;

public class Commands {
    public String getId() {
        return _id;
    }
    public void setId(String id) {
        this._id = id;
    }
    public String getClassName() {
        return _className;
    }
    public void setClassName(String  className) {
        this._className = className;
    }
    public String getFileName() {
        return _filename;
    }
    public void setFileName(String filename) {
        this._filename = filename;
    }
    public Multicommand[] getMulticommands() {

        return _multicommand;

    }



    public void setMulticommands(Multicommand[] multicommand) {

        this._multicommand = multicommand;

    }

    private String _id;
    private String _className;
    private String _filename;
    private Multicommand[] _multicommand;
}
