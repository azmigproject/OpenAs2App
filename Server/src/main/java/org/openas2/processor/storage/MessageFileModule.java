package org.openas2.processor.storage;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openas2.DispositionException;
import org.openas2.OpenAS2Exception;
import org.openas2.WrappedException;
import org.openas2.message.Message;
import org.openas2.params.CompositeParameters;
import org.openas2.params.DateParameters;
import org.openas2.params.InvalidParameterException;
import org.openas2.params.MessageParameters;
import org.openas2.params.ParameterParser;
import org.openas2.params.RandomParameters;
import org.openas2.partner.AS2Partnership;
import org.openas2.processor.receiver.AS2ReceiverModule;
import org.openas2.util.DispositionType;

public class MessageFileModule extends BaseStorageModule {
    public static final String PARAM_HEADER = "header";
    
	private Log logger = LogFactory.getLog(MessageFileModule.class.getSimpleName());


    public void handle(String action, Message msg, Map<Object, Object> options) throws OpenAS2Exception {
        // store message content
        try {

            File msgFile = getFile(msg, getParameter(PARAM_FILENAME, true), action);
            String strSenderID=msg.getPartnership().getSenderID(AS2Partnership.PID_AS2);
            InputStream in = msg.getData().getInputStream();

            if(options!=null) {
                store(msgFile, in, options.get("queueName").toString(),strSenderID, options.get("blobContainer").toString(), Integer.parseInt(options.get("MaxFileSize_Queue").toString()));
                msg.setLogMsg("stored message to  azure for file"+(msg.getPayloadFilename()!=null?msg.getPayloadFilename(): msgFile.getAbsolutePath())+" For Sender="+strSenderID+" "+msg.getLogMsgID());
            }
            else
            {
                store(msgFile, in);
                msg.setLogMsg("stored message for file"+(msg.getPayloadFilename()!=null?msg.getPayloadFilename(): msgFile.getAbsolutePath())+" For Sender="+strSenderID+" "+msg.getLogMsgID());
            }

            logger.info(msg);
        } catch (Exception e) {
            throw new DispositionException(new DispositionType("automatic-action", "MDN-sent-automatically",
                    "processed", "Error", "Error storing transaction"), AS2ReceiverModule.DISP_STORAGE_FAILED, e);
        }

        String headerFilename = getParameter(PARAM_HEADER, false);

        if (headerFilename != null) {
            try {
                File headerFile = getFile(msg, headerFilename, action);
                InputStream in = getHeaderStream(msg);
                store(headerFile, in);
                msg.setLogMsg("stored request headers to " + headerFile.getAbsolutePath()+msg.getLogMsgID());
                logger.info(msg);
            } catch (IOException ioe) {
                throw new WrappedException(ioe);
            }
        }
    }

    protected String getModuleAction() {
        return DO_STORE;
    }


    /**
     * @since 2007-06-01
     */
    protected String getFilename(Message msg, String fileParam, String action) throws InvalidParameterException {
        CompositeParameters compParams = new CompositeParameters(false)
            .add("date", new DateParameters())
        	.add("msg", new MessageParameters(msg))
    	    .add("rand", new RandomParameters());

        return ParameterParser.parse(fileParam, compParams);
    }

    protected InputStream getHeaderStream(Message msg) throws IOException {
        StringBuffer headerBuf = new StringBuffer();

        // write headers to the string buffer
        headerBuf.append("Headers:" + System.getProperty("line.separator"));

        Enumeration<String> headers = msg.getHeaders().getAllHeaderLines();
        String header;

        while (headers.hasMoreElements()) {
            header = (String) headers.nextElement();
            headerBuf.append(header).append(System.getProperty("line.separator"));
        }

        headerBuf.append(System.getProperty("line.separator"));

        // write attributes to the string buffer
        headerBuf.append("Attributes:" + System.getProperty("line.separator"));

        Iterator<Map.Entry<String,String>> attrIt = msg.getAttributes().entrySet().iterator();
        Map.Entry<String,String> attrEntry;

        while (attrIt.hasNext()) {
            attrEntry = attrIt.next();
            headerBuf.append(attrEntry.getKey()).append(": ");
            headerBuf.append(attrEntry.getValue()).append(System.getProperty("line.separator"));
        }

        return new ByteArrayInputStream(headerBuf.toString().getBytes());
    }
}
