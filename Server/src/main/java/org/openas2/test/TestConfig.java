package org.openas2.test;

public class TestConfig
{
	// static String DEFAULT_PARTNER_INFO = "sender.as2_id=OpenAS2A_OID, receiver.as2_id=OpenAS2B_OID";
	public static String DEFAULT_PARTNER_INFO = "sender.as2_id=as1, receiver.as2_id=Maargtestas2";
	// Use extended ASCII characters in the payload
	public static String DEFAULT_MESSAGE_TEXT = "Mañana me voy de viaje. A la luna y más allá.";
	public static String TEST_DATA_BASE_FOLDER = "tests";
	public static String TEST_OUTPUT_FOLDER = TEST_DATA_BASE_FOLDER + "/results";
	public static String TEST_SOURCE_FOLDER = TEST_DATA_BASE_FOLDER + "/data";
	public static String TEST_DEFAULT_SRC_FILE_NAME= "test.msg";
	public static String TEST_DEFAULT_TGT_FILE_NAME= "test.out";

}
