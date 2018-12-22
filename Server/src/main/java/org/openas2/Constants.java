package org.openas2;

import org.joda.time.DateTime;

import org.joda.time.DateTimeZone;
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
    public static String LastUpdateTimeStamp="";
    public static  int DOWNLLOADFILETHRESHOLD=100;
    static long DATETIME_MAXVALUE_TICKS = 3155378975999999999L;
    static long EPOCH_OFFSET =62135596800000L;// Instant.parse("0001-01-01T00:00:00Z").getMillis()


    private static long getTicks(org.joda.time.Instant instant) {
        long seconds = instant.getMillis()+ EPOCH_OFFSET;
        long ticks = seconds* 10000L;
        return ticks+ instant.getMillis() / 1000;
    }

    public static String getNetTicks()
    {
        return String.format("%19d", DATETIME_MAXVALUE_TICKS - getTicks(org.joda.time.DateTime.now(DateTimeZone.UTC).toInstant()));
    }




     /*public static final long DATETIME_MAXVALUE_TICKS = 3155378975999999999L;
    public static final long EPOCH_OFFSET = 62135596800L; // -Instant.parse("0001-01-01T00:00:00Z").getEpochSecond()

   private static long getTicks(Instant instant) {
        long seconds = Math.addExact(instant.getEpochSecond(), EPOCH_OFFSET);
        long ticks = Math.multiplyExact(seconds, 10_000_000L);
        return Math.addExact(ticks, instant.getNano() / 100);
        Datet
    }

    public static String getNetTicks() {
        Instant startTime = Instant.now();
        String s = String.format("%19d", DATETIME_MAXVALUE_TICKS - getTicks(startTime));
        return s;
    }*/
}
