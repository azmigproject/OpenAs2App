package org.openas2;

import org.json.JSONArray;
import org.openas2.lib.dbUtils.partner;

import java.util.ArrayList;
import java.util.List;

public class Constants {

    public  static  String APIURL="http://nptyas2.gcommerceinc.com/api/partnerapi";
    public static  String  STORAGEACCOUNTKEY="";
    public static  String BLOBCONTAINER="";
    public static List<partner> CURRENTPARTNERLIST=new ArrayList<partner>();
    public  static JSONArray APIDataInJASON=null;
    public static  int DOWNLLOADFILETHRESHOLD=128;
}
