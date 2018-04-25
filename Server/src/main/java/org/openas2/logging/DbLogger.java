package org.openas2.logging;

import org.openas2.message.Message;

public class DbLogger extends BaseLogger {

    public String getShowDefaults() {
        return VALUE_SHOW_ALL;
    }

    public void doLog(Throwable t, boolean terminated) {
        addToLoggingTable("Throwable", t.getMessage(), t.getLocalizedMessage());
    }

    public void doLog(Level level, String msgText, Message message) {
        addToLoggingTable(level.getName(), msgText, message.getPayloadFilename());
    }

    private void addToLoggingTable(String type, String msg, String filename) {
        getFormatter().format(level, msgText + "Add From DB Loger"+ (as2Msg == null?"":as2Msg.getLogMsgID()), System.out);
    }
}
