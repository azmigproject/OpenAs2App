package org.openas2;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTimeZone;
import org.json.JSONArray;
import org.openas2.lib.dbUtils.Profile;
import org.openas2.lib.dbUtils.partner;
import org.openas2.message.Message;
import org.openas2.message.MessageMDN;
import org.openas2.partner.AS2Partnership;
import java.util.ArrayList;
import java.util.List;



public class Constants {

    public  static  String APIURL="http://nptyas2.gcommerceinc.com/api/partnerapi";
    public static  String  STORAGEACCOUNTKEY="";
    public static  String BLOBCONTAINER="";
    public static List<partner> CURRENTPARTNERLIST=new ArrayList<partner>();
    public static List<String> ACTIVEPARTNERCERTALIAS=new ArrayList<String>();
    public static  List<Profile> AllProfiles=new ArrayList<Profile>();
    public static Profile MainProfile=null;
    public  static JSONArray APIDataInJASON=null;
    public static String LastUpdateTimeStamp="";
    public static String As2IdSeperatorString="_n_";
    public static  int DOWNLLOADFILETHRESHOLD=100;
    static long DATETIME_MAXVALUE_TICKS = 3155378975999999999L;
    static long EPOCH_OFFSET =62135596800000L;// Instant.parse("0001-01-01T00:00:00Z").getMillis()



    private static long getTicks(org.joda.time.Instant instant) {
        long seconds = instant.getMillis()+ EPOCH_OFFSET;
        long ticks = seconds* 10000L;
        return ticks+ instant.getMillis() / 1000;
    }

    public static String[] GetAS2IDsBasedOnSeperator(String PartnerID)
    {
        Log logger = LogFactory.getLog(Constants.class.getSimpleName());
       String[] returnString=null;
        if(PartnerID.lastIndexOf(As2IdSeperatorString)>0)
        {
            returnString=new String[2];
            returnString[0]= PartnerID.split(As2IdSeperatorString)[0];
            returnString[1]= PartnerID.split(As2IdSeperatorString)[1];
            logger.info("GetAS2IDsBasedOnSeperator-As2ID1" + returnString[0]);
            logger.info("GetAS2IDsBasedOnSeperator-As2ID2" + returnString[1]);
        }
        else
        {
            returnString=new String[1];
            returnString[0]=PartnerID;
            logger.info("GetAS2IDsBasedOnSeperator-As2ID1" + returnString[0]);
        }



        return returnString;
    }

    public static void UpdateMsgSenderPartnership(Message msg, String passedAS2ID, boolean IsMsgSendingFromProfile)

    {
        Log logger = LogFactory.getLog(Constants.class.getSimpleName());
        if (logger.isInfoEnabled())
            logger.info("passed AS2ID: " + passedAS2ID);
     if(!Constants.MainProfile.getAS2Idenitfier().trim().equalsIgnoreCase(passedAS2ID.trim())) {
         if (msg != null) {
             Profile profile = null;
             for (int count = 0; count < Constants.AllProfiles.size(); count++) {
                 profile = Constants.AllProfiles.get(count);
                 String strProfile = profile.getAS2Idenitfier().trim();
                 if (strProfile.equalsIgnoreCase(passedAS2ID.trim())) {
                     // profile=Constants.AllProfiles.get(count);
                     logger.info("passed AS2ID found in profile: " + profile.getAS2Idenitfier() + "from given profile");
                     break;
                 } else {
                     logger.info("passed AS2ID not found in profile with id:" + profile.getAS2Idenitfier());

                     profile = null;
                 }

             }

             if (profile != null) {
                 logger.info("Profile found for passedAs2Id");
                 if (logger.isInfoEnabled()) {
                     logger.info("passed AS2ID: " + profile.getAS2Idenitfier());
                     logger.info("passed email ID: " + profile.getEmailAddress());
                 }
                 if (IsMsgSendingFromProfile) {
                     //In-Outgoing Case
                     msg.getPartnership().setSenderID(AS2Partnership.PID_AS2, profile.getAS2Idenitfier());
                     msg.getPartnership().setAttribute("as2_mdn_to", profile.getEmailAddress());
                     msg.setHeader("AS2-From", profile.getAS2Idenitfier());

                 } else {
                     //In-Incoming Case

                     msg.getPartnership().setReceiverID(AS2Partnership.PID_AS2, profile.getAS2Idenitfier());
                     msg.getPartnership().setAttribute("as2_mdn_to", profile.getEmailAddress());
                     msg.setHeader("AS2-To", profile.getAS2Idenitfier());
                 }
                 if (logger.isInfoEnabled()) {
                     logger.info("sender Id in message is : " + msg.getPartnership().getSenderID(AS2Partnership.PID_AS2));
                     logger.info("as2_mdn_to " + msg.getPartnership().getAttribute("as2_mdn_to"));
                 }
             } else {
                 logger.info("Profile not Found checking list of profile available");
                 for (int count = 0; count < Constants.AllProfiles.size(); count++) {
                     profile = Constants.AllProfiles.get(count);
                     logger.info("profile found as  " + profile.getAS2Idenitfier());


                 }
             }
         }
     }
    }


    public static void UpdateMsgSenderPartnership(MessageMDN msg, String passedAS2ID, boolean IsMsgSendingFromProfile)

    {
        Log logger = LogFactory.getLog(Constants.class.getSimpleName());
        if (logger.isInfoEnabled())
            logger.info("passed AS2ID: " + passedAS2ID);
        if(!Constants.MainProfile.getAS2Idenitfier().trim().equalsIgnoreCase(passedAS2ID.trim())) {
            if (msg != null) {
                Profile profile = null;
                for (int count = 0; count < Constants.AllProfiles.size(); count++) {
                    profile = Constants.AllProfiles.get(count);
                    String strProfile = profile.getAS2Idenitfier().trim();
                    if (strProfile.equalsIgnoreCase(passedAS2ID.trim())) {
                        // profile=Constants.AllProfiles.get(count);
                        logger.info("passed AS2ID found in profile: " + profile.getAS2Idenitfier() + "from given profile");
                        break;
                    } else {
                        logger.info("passed AS2ID not found in profile with id:" + profile.getAS2Idenitfier());

                        profile = null;
                    }

                }

                if (profile != null) {
                    logger.info("Profile found for passedAs2Id");
                    if (logger.isInfoEnabled()) {
                        logger.info("passed AS2ID: " + profile.getAS2Idenitfier());
                        logger.info("passed email ID: " + profile.getEmailAddress());
                    }
                    if (IsMsgSendingFromProfile) {
                        //In-Outgoing Case
                        msg.getPartnership().setSenderID(AS2Partnership.PID_AS2, profile.getAS2Idenitfier());
                        msg.getPartnership().setAttribute("as2_mdn_to", profile.getEmailAddress());
                        msg.setHeader("AS2-From", profile.getAS2Idenitfier());

                    } else {
                        //In-Incoming Case

                        msg.getPartnership().setReceiverID(AS2Partnership.PID_AS2, profile.getAS2Idenitfier());
                        msg.getPartnership().setAttribute("as2_mdn_to", profile.getEmailAddress());
                        msg.setHeader("AS2-To", profile.getAS2Idenitfier());
                    }
                    if (logger.isInfoEnabled()) {
                        logger.info("sender Id in message is : " + msg.getPartnership().getSenderID(AS2Partnership.PID_AS2));
                        logger.info("as2_mdn_to " + msg.getPartnership().getAttribute("as2_mdn_to"));
                    }
                } else {
                    logger.info("Profile not Found checking list of profile available");
                    for (int count = 0; count < Constants.AllProfiles.size(); count++) {
                        profile = Constants.AllProfiles.get(count);
                        logger.info("profile found as  " + profile.getAS2Idenitfier());


                    }
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
