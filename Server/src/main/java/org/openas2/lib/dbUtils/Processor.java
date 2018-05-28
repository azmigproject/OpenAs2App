package org.openas2.lib.dbUtils;

public class Processor {
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
    public module[] getModules() {

        return _modules;

    }



    public void setModules(module[] modules) {

        this._modules = modules;

    }

    private String _id;
    private String _classname;
    private  String _pendingMDN;
    private  String _pendingMDNInfo;
    private module[] _modules;



}
