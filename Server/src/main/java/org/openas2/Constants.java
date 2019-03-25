package org.openas2;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTimeZone;
import org.json.JSONArray;
import org.openas2.lib.dbUtils.Profile;
import org.openas2.lib.dbUtils.partner;
import org.openas2.message.Message;
import org.openas2.partner.AS2Partnership;
import java.util.ArrayList;
import java.util.List;



public class Constants {

    public  static  String APIURL="http://nptyas2.gcommerceinc.com/api/partnerapi";
    public static  String  STORAGEACCOUNTKEY="";
    public static  String BLOBCONTAINER="";
    public static List<partner> CURRENTPARTNERLIST=new ArrayList<partner>();
    public static  List<Profile> AllProfiles=new ArrayList<Profile>();
    public static Profile MainProfile=null;
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

    public static void UpdateMsgSenderPartnership(Message msg, String passedAS2ID)
    {
        Log logger = LogFactory.getLog(Constants.class.getSimpleName());
        if (logger.isInfoEnabled())
            logger.info("passed AS2ID: " + passedAS2ID);

        if(msg!=null)
        {
            Profile profile=null;
            for(int count=0;count<Constants.AllProfiles.size();count++)
            {
                if(Constants.AllProfiles.get(count).getAS2Idenitfier()==passedAS2ID)
                {
                    profile=Constants.AllProfiles.get(count);
                    break;
                }
            }

            if(profile!=null)
            {
                if (logger.isInfoEnabled()) {
                    logger.info("passed AS2ID: " + profile.getAS2Idenitfier());
                    logger.info("passed email ID: " + profile.getEmailAddress());
                }
                msg.getPartnership().setSenderID(AS2Partnership.PID_AS2,profile.getAS2Idenitfier());
                msg.getPartnership().setAttribute("as2_mdn_to",profile.getEmailAddress());

                if (logger.isInfoEnabled()) {
                    logger.info("sender Id in message is : " + msg.getPartnership().getSenderID(AS2Partnership.PID_AS2));
                    logger.info("as2_mdn_to " + msg.getPartnership().getAttribute("as2_mdn_to"));
                }
            }
        }
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
