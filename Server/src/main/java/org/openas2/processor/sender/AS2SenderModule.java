package org.openas2.processor.sender;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Locale;
import java.util.Map;

import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.net.ssl.SSLHandshakeException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openas2.OpenAS2Exception;
import org.openas2.Session;
import org.openas2.cert.CertificateFactory;
import org.openas2.lib.helper.ICryptoHelper;
import org.openas2.message.AS2Message;
import org.openas2.message.AS2MessageMDN;
import org.openas2.message.DataHistoryItem;
import org.openas2.message.FileAttribute;
import org.openas2.message.Message;
import org.openas2.message.MessageMDN;
import org.openas2.message.NetAttribute;
import org.openas2.params.InvalidParameterException;
import org.openas2.partner.AS2Partnership;
import org.openas2.partner.Partnership;
import org.openas2.partner.SecurePartnership;
import org.openas2.processor.resender.ResenderModule;
import org.openas2.util.*;

public class AS2SenderModule extends HttpSenderModule {

    private Log logger = LogFactory.getLog(AS2SenderModule.class.getSimpleName());

    public boolean canHandle(String action, Message msg, Map<Object, Object> options)
    {
        if (!action.equals(SenderModule.DO_SEND))
        {
            return false;
        }

        return (msg instanceof AS2Message);
    }

    @SuppressWarnings("unchecked")
    public void handle(String action, Message msg, Map<Object, Object> options) throws OpenAS2Exception
    {

        if (logger.isInfoEnabled())
        {
            logger.info("message sender invoked" + msg.getLogMsgID());
        }
        boolean isResend = Message.MSG_STATUS_MSG_RESEND.equals(msg.getStatus());
        options.put("DIRECTION", "SEND");
        options.put("IS_RESEND", isResend ? "Y" : "N");
        if (!(msg instanceof AS2Message))
        {
            throw new OpenAS2Exception("Can't send non-AS2 message");
        }

        // verify all required information is present for sending
        checkRequired(msg);
        // Store options on the message object
        if (options != null)
        {
            msg.getOptions().putAll(options);
        }
        if (logger.isTraceEnabled())
        {
            logger.trace("Retry count from options: " + options);
        }
        // Get the resend retry count
        String retries = AS2Util.retries(options, getParameter(SenderModule.SOPT_RETRIES, false));

        // Get any static custom headers
        String customHeaders = msg.getPartnership().getAttribute(AS2Partnership.PA_CUSTOM_MIME_HEADERS);
        if (customHeaders != null && customHeaders.length() > 0)
        {
            if (logger.isTraceEnabled())
            {
                logger.trace("Adding custom header attribute to custom headers map..." + msg.getLogMsgID());
            }
            String[] headers = customHeaders.split("\\s*;\\s*");
            for (int i = 0; i < headers.length; i++)
            {
                String[] header = headers[i].split("\\s*:\\s*");
                if (logger.isTraceEnabled())
                {
                    logger.trace("Adding custom header: " + headers[i]
                            + " :::Split count:" + header.length + msg.getLogMsgID());
                }
                if (header.length != 2)
                {
                    throw new OpenAS2Exception("Invalid custom header: " + headers[i]);
                }
                msg.addCustomOuterMimeHeader(header[0].replaceAll(" ", ""), header[1]);
            }
        }
        // encrypt and/or sign and/or compress the message if needed
        MimeBodyPart securedData;
        try
        {
            securedData = secure(msg);
            //Add any additional headers since this will be the outermost Mime body part if configured
            addCustomOuterMimeHeaders(msg, securedData);

            storePendingInfo((AS2Message) msg, isResend);
        } catch (Exception e)
        {
            msg.setLogMsg(org.openas2.logging.Log.getExceptionMsg(e));
            logger.error(msg, e);
            // Log significant msg state
            msg.setOption("STATE", Message.MSG_STATE_SEND_EXCEPTION);
            msg.trackMsgState(getSession());
            throw new OpenAS2Exception("Error setting up message for sending.", e);
        }
        if (logger.isTraceEnabled())
        {
            try
            {
                logger.trace("Message object in sender module. Content-Disposition: " + msg.getContentDisposition()
                        + "\n      Content-Type : " + msg.getContentType() + "\n      HEADERS : " + AS2Util.printHeaders(msg.getData().getAllHeaders())
                        + "\n      Content-Disposition in MSG getData() MIMEPART: " + msg.getData().getContentType()
                        + msg.getLogMsgID());
            } catch (Exception e)
            {
            }
        }
        
        HttpURLConnection conn = null;
        InputStream connIn = null;
        
        try
        {
            try
            {
                // Create the HTTP connection and set up headers
                String url = msg.getPartnership().getAttribute(AS2Partnership.PA_AS2_URL);
                logger.info("Partnership Data - " + msg.getPartnership().toString());
                conn = getConnection(url, true, true, false, "POST", 180000, 180000);
                logger.info("*****Connection/Read Timeout Value = Connection:" +conn.getConnectTimeout()+ " - Read:"+conn.getReadTimeout()+" - "+msg.getLogMsgID());
                
                
                // Log significant msg state
                msg.setOption("STATE", Message.MSG_STATE_SEND_START);
                msg.trackMsgState(getSession());
                //Retry Interval and Retry Attempts
                logger.info("Sending Message over AS2 Stage 1" +msg.getLogMsgID());
                sendMessage(conn, msg, securedData, retries);
                logger.info("Sending Message over AS2 Stage 2" +msg.getLogMsgID());
            } catch (HttpResponseException hre)
            {
            	logger.info("Error over AS2 Stage 2.1" +msg.getLogMsgID());
                // Will have been logged so just resend
                resend(msg, hre, retries);
                // Log significant msg state
                msg.setOption("STATE", Message.MSG_STATE_SEND_EXCEPTION);
                msg.trackMsgState(getSession());
                return;
            } catch (SSLHandshakeException e)
            {
            	logger.info("Error over AS2 Stage 2.2" +msg.getLogMsgID());
                msg.setLogMsg("Failed to connect to partner using SSL certificate. Please run the SSL certificate checker utility to identify the issue: " + conn.getURL());
                logger.error(msg, e);
                msg.setOption("STATE", Message.MSG_STATE_SEND_FAIL);
                msg.trackMsgState(getSession());
                return;
            } catch (Exception e)
            {
            	logger.info("Error over AS2 Stage 2.3" +msg.getLogMsgID());
                msg.setLogMsg("Unexpected error sending file: " + org.openas2.logging.Log.getExceptionMsg(e));
                logger.error(msg, e);
                resend(msg, new OpenAS2Exception(org.openas2.logging.Log.getExceptionMsg(e)), retries);
                // Log significant msg state
                msg.setOption("STATE", Message.MSG_STATE_SEND_EXCEPTION);
                msg.trackMsgState(getSession());
                return;
            }

            if (logger.isInfoEnabled())
            {
                logger.info("Message sent. Checking if MDN will be returned..." + msg.getLogMsgID());
            }
            // Receive an MDN
            if (msg.isConfiguredForMDN())
            {
            	logger.info("Checking MDN NOW Stage1..." + msg.getLogMsgID());
                msg.setStatus(Message.MSG_STATUS_MDN_WAIT);
                // Check if it will be an AsyncMDN
                if (msg.getPartnership().getAttribute(AS2Partnership.PA_AS2_RECEIPT_OPTION) == null)
                {
                	logger.info("Checking MDN NOW Stage2..." + msg.getLogMsgID());
                    if (logger.isTraceEnabled())
                    {
                        logger.trace("Waiting for synchronous MDN response..." + msg.getLogMsgID());
                    }
                    // Create a MessageMDN and copy HTTP headers
                    if (logger.isTraceEnabled())
                    {
                        logger.trace("Awaiting sync MDN. Orig msg contains headers:" + AS2Util.printHeaders(msg.getHeaders().getAllHeaders()) + msg.getLogMsgID());
                    }
                    
                    logger.info("Checking MDN NOW Stage3..." + msg.getLogMsgID());
                    MessageMDN mdn = new AS2MessageMDN((AS2Message) msg, false);
                    logger.info("Checking MDN NOW Stage4..." + msg.getLogMsgID());
                    
                    if (logger.isTraceEnabled())
                    {
                        logger.trace("MDN msg initalised for inbound contains headers:" + AS2Util.printHeaders(mdn.getHeaders().getAllHeaders()) + msg.getLogMsgID());
                    }
                    
                    logger.info("Checking MDN NOW Stage5..." + msg.getLogMsgID());
                    try{
                    	
                        HTTPUtil.copyHttpHeaders(conn, mdn.getHeaders(), msg);
                        
                        logger.info("Http Copy Headers Stag 5.0" +msg.getLogMsgID());
                    }catch(Exception ex)
                    {
                    	 msg.setLogMsg("Failed to get input stream for receiving MDN: "
                                 + org.openas2.logging.Log.getExceptionMsg(ex));
                         logger.error(msg, ex);
                    }
                    
                    // Receive the MDN data
                    //InputStream connIn = null;
                    try
                    {
                    	logger.info("Checking MDN NOW Stage6..." + msg.getLogMsgID());
                        connIn = conn.getInputStream();
                        logger.info("Checking MDN NOW Stage7..." + msg.getLogMsgID());
                    } catch (IOException e1)
                    {
                        msg.setLogMsg("Failed to get input stream for receiving MDN: "
                                + org.openas2.logging.Log.getExceptionMsg(e1));
                        logger.error(msg, e1);
                        //Commented to remove Duplicates due to error in MDN Receipt processing
                        // resend(msg, new OpenAS2Exception(org.openas2.logging.Log.getExceptionMsg(e1)), retries);
                        // Log significant msg state
                        msg.setOption("STATE", Message.MSG_STATE_MDN_RECEIVING_EXCEPTION);
                        msg.trackMsgState(getSession());
                    }
                   // ByteArrayOutputStream mdnStream = new ByteArrayOutputStream();//TODO changed to a ByteArray and called a convertion method below
                   
                   // byte[] mdnStream = null;
//                    try
//                    {
                    	logger.info("Copy MDN Stag 4" +msg.getLogMsgID());
                    	//byte[] mdnStream = null;
                    	
//                        String contentLength = mdn.getHeader("Content-Length");
                        // Retrieve the message content
//                       if (contentLength != null)//TODO remove if/else as it was not relevant
//                        {
//                        	logger.info("Copy MDN Stag 5" +msg.getLogMsgID());
//                            try
//                            {

////                                IOUtils.copy(connIn, mdnStream);
//                            } catch (IOException nfe)//TODO changed from NumberFormatException to IOException cause copy only returns IOException
//                            {
//////                                IOUtils.copy(connIn, mdnStream);
//                            }
//                        } else
//                        {
//                            //IOUtils.copy(connIn, mdnStream);
//                        }
                    	//TODO Convert from InputStream directly to byteArray
                       // mdnStream = IOUtils.toByteArray(connIn);
                       
                        // byte[] bytes = mdnStream.toByteArray();

                        /*try {

                            //blobHelper.UploadFileInBlob(msg.getPartnership().getAttribute("blobContainer"), msg.getMDN().getMessageID(), bytes);
                        }
                        catch (Exception exp)
                        {
                            msg.setLogMsg("IO exception receiving MDN: "
                                    + org.openas2.logging.Log.getExceptionMsg(exp));
                            logger.error(msg, exp);
                            // What to do???
                            resend(msg, new OpenAS2Exception(org.openas2.logging.Log.getExceptionMsg(exp)), retries);
                        }*/
//                    } catch (Exception ioe)
//                    {
//                    	logger.info("Copy MDN Stag 6.0" +msg.getLogMsgID());
//                        msg.setLogMsg("IO exception receiving MDN: "
//                                + org.openas2.logging.Log.getExceptionMsg(ioe));
//                        logger.error(msg, ioe);
//                        // What to do???
//                        //resend(msg, new OpenAS2Exception(org.openas2.logging.Log.getExceptionMsg(ioe)), retries);
//                        // Log significant msg state
//                        msg.setOption("STATE", Message.MSG_STATE_MDN_RECEIVING_EXCEPTION);
//                        msg.trackMsgState(getSession());
//                   
//                    }finally
//                    {
//                    	logger.info("Finally Child MDN Stag 6.1" +msg.getLogMsgID());
//                        try
//                        {
//                            if (connIn != null)
//                            {
//                            	logger.info("Finally Child MDN Stag 7" +msg.getLogMsgID());
//                                connIn.close();
//                            }
//                        } catch (IOException e)
//                        {
//                        	logger.info("Finally Child MDN Connection Close Stag 8" +msg.getLogMsgID());
//                        }
//                    }

                    if (logger.isInfoEnabled())
                    {
                        logger.info("Synchronous MDN received. Start processing..." + msg.getLogMsgID());
                    }
                    msg.setStatus(Message.MSG_STATUS_MDN_PROCESS_INIT);
                    try
                    {
                    	logger.info("Process MDN StaRT 9" +msg.getLogMsgID());
                       // AS2Util.processMDN((AS2Message) msg, mdnStream.toByteArray(), null, false, getSession(), this);//TODO changed to the line below to use converted inputstream to bytarray object
                        
                    	//AS2Util.processMDN((AS2Message) msg, IOUtils.toByteArray(connIn), null, false, getSession(), this);
                    	AS2Util.processMDN((AS2Message) msg, toByteArrayInputStream(connIn, msg), null, false, getSession(), this);
                    	logger.info("Checking Closed MDN InputStream 9.0.3" + msg.getLogMsgID());
                    	if (connIn != null)
                    	{
                    		connIn.close();
                            connIn = null;
                    	}
                        // Log significant msg state
                       // BlobHelper blobHelper=new BlobHelper();
                        //blobHelper.UploadFileInBlob(msg.getPartnership().getAttribute("blobContainer"), msg.getMDN().getMessageID(), mdnStream.toByteArray());
                        msg.setOption("STATE", Message.MSG_STATE_MSG_SENT_MDN_RECEIVED_OK);
                        msg.trackMsgState(getSession());
                    } catch (Exception e)
                    {
                    	logger.info("Process MDN StaRT 9.1" +msg.getLogMsgID());
                        if (Message.MSG_STATUS_MDN_PROCESS_INIT.equals(msg.getStatus())
                                || Message.MSG_STATUS_MDN_PARSE.equals(msg.getStatus())
                                || !(e instanceof OpenAS2Exception))
                        {
                            /*
							 * Cannot identify the target if in init or parse
							 * state so not sure what the best course of action
							 * is apart from do nothing
							 */
                        	logger.info("MDN Exception" +msg.getLogMsgID()+ " - "+e.getMessage());
                            msg.setLogMsg("Unhandled error condition receiving synchronous MDN. Message and asociated files cleanup will be attempted but may be in an unknown state.");
                            logger.error(msg, e);
                        }
                        /*
						 * Most likely a resend abort of max resend reached if
						 * OpenAS2Exception so do not log as should have been
						 * logged upstream ... just clean up the mess
						 */
                        else
                        {
                            // Must have received MDN successfully
                            msg.setLogMsg("Exception receiving synchronous MDN. Message and asociated files cleanup will be attempted but may be in an unknown state.");
                            logger.error(msg, e);

                        }
                        // Log significant msg state
                        msg.setOption("STATE", Message.MSG_STATE_SEND_FAIL);
                        msg.trackMsgState(getSession());
                        AS2Util.cleanupFiles(msg, true);
                    }
                    logger.info("Process MDN Completed 10" +msg.getLogMsgID());
                    
                    
                }
            }

        } finally
        {
        	
        	
        	logger.info("Process MDN StaRT 11" +msg.getLogMsgID());
        	
        	if (connIn != null)
        	{
        		try
        		{
        			connIn.close();
        			connIn = null;
                }catch(IOException cio)
        		{
                	logger.info("Process MDN StaRT 11" +msg.getLogMsgID() + " - "+ cio.getMessage());
        		}
        	}    		
        	
        	if (conn != null)
            {
            	logger.info("Process MDN StaRT 12" +msg.getLogMsgID());
                conn.disconnect();
                conn = null;
            }
        }
    }

    protected void checkRequired(Message msg) throws InvalidParameterException
    {
        Partnership partnership = msg.getPartnership();

        try
        {
            InvalidParameterException.checkValue(msg, "ContentType", msg.getContentType());
            InvalidParameterException.checkValue(msg, "Attribute: " + AS2Partnership.PA_AS2_URL,
                    partnership.getAttribute(AS2Partnership.PA_AS2_URL));
            InvalidParameterException.checkValue(msg, "Receiver: " + AS2Partnership.PID_AS2,
                    partnership.getReceiverID(AS2Partnership.PID_AS2));
            InvalidParameterException.checkValue(msg, "Sender: " + AS2Partnership.PID_AS2,
                    partnership.getSenderID(AS2Partnership.PID_AS2));
            InvalidParameterException.checkValue(msg, "Subject", msg.getSubject());
            InvalidParameterException.checkValue(msg, "Sender: " + Partnership.PID_EMAIL,
                    partnership.getSenderID(Partnership.PID_EMAIL));
            InvalidParameterException.checkValue(msg, "Message Data", msg.getData());
        } catch (InvalidParameterException rpe)
        {
            rpe.addSource(OpenAS2Exception.SOURCE_MESSAGE, msg);
            logger.info("Excepton at checkRequired"+ OpenAS2Exception.SOURCE_MESSAGE);
            throw rpe;
        }
    }



    private void sendMessage(HttpURLConnection conn, Message msg, MimeBodyPart securedData, String retries)
	            throws Exception
	    {
	        logger.info("Start message sending with"+conn.getURL() + msg.getLogMsgID());
	        updateHttpHeaders(conn, msg, securedData);
            LogHttpHeadersInBlob(conn, msg, securedData);
	        msg.setAttribute(NetAttribute.MA_DESTINATION_IP, conn.getURL().getHost());
	        msg.setAttribute(NetAttribute.MA_DESTINATION_PORT, Integer.toString(conn.getURL().getPort()));
	        logger.info("set Header and update");
	        if (logger.isInfoEnabled())
	        {
	            logger.info("Connecting to: " + conn.getURL() + msg.getLogMsgID());
	        }
	        int maxRetryAttempts = getSession().getRetryAttempts();
	        int retryIntervalInSeconds = getSession().getRetryIntervalInSeconds();
	        int retryAttempts = 0;
	        OutputStream messageOut = null;
	        // Note: closing this stream causes connection abort errors on some AS2
	        // servers
	        while(retryAttempts < maxRetryAttempts) {
	            ++retryAttempts;
	            try {
	                messageOut = conn.getOutputStream();
	                retryAttempts = maxRetryAttempts;
	            } catch (Exception ex){
	                Thread.sleep(retryIntervalInSeconds * 1000);
	            }
	        }
	        retryAttempts = 0;
	        InputStream messageIn = null;
	        // Transfer the data
	        while (retryAttempts < maxRetryAttempts) {
	            ++retryAttempts;
	            try {
	                messageIn = securedData.getInputStream();
	                retryAttempts = maxRetryAttempts;
	            } catch (Exception ex){
	                Thread.sleep(retryIntervalInSeconds * 1000);
	            }
	        }
	        try
	        {
	            ProfilerStub transferStub = Profiler.startProfile();
	
	            int bytes = IOUtils.copy(messageIn, messageOut);
	            //int bytes = messageIn.available();
	
	            
	            Profiler.endProfile(transferStub);
	            if (logger.isInfoEnabled())
	            {
	                logger.info("transferred " + IOUtilOld.getTransferRate(bytes, transferStub) + msg.getLogMsgID());
	            }
	         }catch(Exception e3) //TODO added catch to manage errors from above 
	          {
	            	 logger.info("Error transmitting AS2message **** " + msg.getLogMsgID() + " - "+e3.getMessage());
	          } finally
		         {
		        	try
		        	{
		        		messageIn.close();
		        		messageIn = null;
		        		
		        		messageOut.flush();
		        		messageOut.close();
		        	
		        	}catch(IOException ioe2)//TODO added catch to manage errors from close statement
		        	{
		        		 logger.info("Error closing messageIn **** " + msg.getLogMsgID() + " - "+ioe2.getMessage());
		        	}
		        }
	        // Check the HTTP Response code
	//        int rc = 0;
	//        try
	//        {
	//        	logger.info("Get Send Response Code Stage 1 **** " + msg.getLogMsgID() );
	//        	rc = conn.getResponseCode();
	//            logger.info("Get Send Response Code Stage 2 **** " + msg.getLogMsgID() );
	//            
	//        logger.info("REVIEW message response Code. URL: " + conn.getURL().toString() + " ::: Response Code: " + rc
	//                + " ::: Response Message: " + conn.getResponseMessage() + " - "+msg.getLogMsgID());
	//        }catch(Exception re)
	//        {
	//        	logger.info("Error Receiving Send Response Code **** " + msg.getLogMsgID() + " - "+re.getMessage());
	//        	throw re;
	//        }
	//        
	//        if ((rc != HttpURLConnection.HTTP_OK) && (rc != HttpURLConnection.HTTP_CREATED)
	//                && (rc != HttpURLConnection.HTTP_ACCEPTED) && (rc != HttpURLConnection.HTTP_PARTIAL)
	//                && (rc != HttpURLConnection.HTTP_NO_CONTENT))
	//        {
	//            msg.setLogMsg("Error sending message. URL: " + conn.getURL().toString() + " ::: Response Code: " + rc
	//                    + " ::: Response Message: " + conn.getResponseMessage());
	//            logger.error(msg);
	//            throw new HttpResponseException(conn.getURL().toString(), rc, conn.getResponseMessage());
	//        }
	        logger.info("AS2 Sending is complete **** " + msg.getLogMsgID());
	    }

	private void resend(Message msg, OpenAS2Exception cause, String tries) throws OpenAS2Exception
    {
        AS2Util.resend(getSession(), this, SenderModule.DO_SEND, msg, cause, tries, false);
    }

    /**
     * Returns a MimeBodyPart or MimeMultipart object
     * @param msg The message object carried around containing necessary information
     * @return The secured mimebodypart
     * @throws Exception some unforseen issue has occurred
     */
    protected MimeBodyPart secure(Message msg) throws Exception
    {
        // Set up encrypt/sign variables
        MimeBodyPart dataBP = msg.getData();
        /*
		 * Based on RFC4130, RFC6362 and RFC5042, the MIC is calculated as
		 * follows: Signed message - MIME header fields and content that is to
		 * be signed which may or may not be encrypted and/or compressed.
		 * 
		 * Unsigned encrypted message - data content including all MIME header
		 * fields and any applied Content-Transfer-Encoding prior to encryption
		 * and/or compression
		 * 
		 * So essentially, calculate the MIC before doing any compression or
		 * encryption if message not being signed otherwise calculate right
		 * before signing of the message but include headers for unsigned
		 * messages (see RFC4130 section 7.3.1 for details)
		 */

        Partnership partnership = msg.getPartnership();
        String contentTxfrEncoding = partnership.getAttribute(Partnership.PA_CONTENT_TRANSFER_ENCODING);
        if (contentTxfrEncoding == null)
        {
            contentTxfrEncoding = Session.DEFAULT_CONTENT_TRANSFER_ENCODING;
        }

        boolean encrypt = partnership.getAttribute(SecurePartnership.PA_ENCRYPT) != null;
        boolean sign = partnership.getAttribute(SecurePartnership.PA_SIGN) != null;

        if (!sign)
        {
            calcAndStoreMic(msg, dataBP, (sign || encrypt));
        }

        // Check if compression is enabled
        String compressionType = msg.getPartnership().getAttribute("compression_type");
        if (logger.isTraceEnabled())
        {
            logger.trace("Compression type from config: " + compressionType);
        }
        boolean isCompress = false;
        if (compressionType != null && !"NONE".equalsIgnoreCase(compressionType))
        {
            if (compressionType.equalsIgnoreCase(ICryptoHelper.COMPRESSION_ZLIB))
            {
                isCompress = true;
            } 
            else
            {
                throw new OpenAS2Exception("Unsupported compression type: " + compressionType);
            }
        }
        String compressionMode = msg.getPartnership().getAttribute("compression_mode");
        boolean isCompressBeforeSign = true; // Defaults to compressing the
        // entire message before signing
        // and encryption
        if (compressionMode != null && compressionMode.equalsIgnoreCase("compress-after-signing"))
        {
            isCompressBeforeSign = false;
        }
        if (isCompress && isCompressBeforeSign)
        {
            if (logger.isTraceEnabled())
            {
                logger.trace("Compressing outbound message before signing...");
            }
            if (!sign && !encrypt)
            {
                //Add any additional headers since this will be the outermost Mime body part if configured
                addCustomOuterMimeHeaders(msg, dataBP);
            }
            dataBP = AS2Util.getCryptoHelper().compress(msg, dataBP, compressionType, contentTxfrEncoding);
        }
        // Encrypt and/or sign the data if requested
        CertificateFactory certFx = getSession().getCertificateFactory();

        // Sign the data if requested
        if (sign)
        {
            if (!encrypt && !(isCompress && !isCompressBeforeSign))
            {
                //Add any additional headers since this will be the outermost Mime body part if configured
                addCustomOuterMimeHeaders(msg, dataBP);
            }
            calcAndStoreMic(msg, dataBP, (sign || encrypt));
            X509Certificate senderCert = certFx.getCertificate(msg, Partnership.PTYPE_SENDER);

            PrivateKey senderKey = certFx.getPrivateKey(msg, senderCert);
            String digest = partnership.getAttribute(SecurePartnership.PA_SIGN);

            if (logger.isDebugEnabled())
            {
                logger.debug("Params for creating signed body part:: DATA: " + dataBP + "\n SIGN DIGEST: " + digest
                        + "\n CERT ALG NAME EXTRACTED: " + senderCert.getSigAlgName()
                        + "\n CERT PUB KEY ALG NAME EXTRACTED: " + senderCert.getPublicKey().getAlgorithm()
                        + msg.getLogMsgID());
            }
            boolean isRemoveCmsAlgorithmProtectionAttr = "true".equalsIgnoreCase(partnership.getAttribute(Partnership.PA_REMOVE_PROTECTION_ATTRIB));
            dataBP = AS2Util.getCryptoHelper().sign(dataBP, senderCert, senderKey, digest
                    , contentTxfrEncoding, msg.getPartnership().isRenameDigestToOldName(), isRemoveCmsAlgorithmProtectionAttr);

            DataHistoryItem historyItem = new DataHistoryItem(dataBP.getContentType());
            // *** add one more item to msg history
            msg.getHistory().getItems().add(historyItem);

            if (logger.isDebugEnabled())
            {
                logger.debug("signed data" + msg.getLogMsgID());
            }
        }

        if (isCompress && !isCompressBeforeSign)
        {
            if (!encrypt)
            {
                //Add any additional headers since this will be the outermost Mime body part if configured
                addCustomOuterMimeHeaders(msg, dataBP);
            }
            if (logger.isTraceEnabled())
            {
                logger.trace("Compressing outbound message after signing...");
            }
            dataBP = AS2Util.getCryptoHelper().compress(msg, dataBP, compressionType, contentTxfrEncoding);
        }
        // Encrypt the data if requested
        if (encrypt)
        {
            //Add any additional headers since this will be the outermost Mime body part if configured
            addCustomOuterMimeHeaders(msg, dataBP);
            String algorithm = partnership.getAttribute(SecurePartnership.PA_ENCRYPT);

            X509Certificate receiverCert = certFx.getCertificate(msg, Partnership.PTYPE_RECEIVER);
            dataBP = AS2Util.getCryptoHelper().encrypt(dataBP, receiverCert, algorithm, contentTxfrEncoding);

            // Asynch MDN 2007-03-12
            DataHistoryItem historyItem = new DataHistoryItem(dataBP.getContentType());
            // *** add one more item to msg history
            msg.getHistory().getItems().add(historyItem);

            if (logger.isDebugEnabled())
            {
                logger.debug("encrypted data" + msg.getLogMsgID());
            }
        }

        String t = dataBP.getEncoding();
        if ((t == null || t.length() < 1) && "true".equalsIgnoreCase(partnership.getAttribute(Partnership.PA_SET_CONTENT_TRANSFER_ENCODING_OMBP)))
        {
            dataBP.setHeader("Content-Transfer-Encoding", contentTxfrEncoding);
        }
        return dataBP;
    }

    protected void addCustomOuterMimeHeaders(Message msg, MimeBodyPart dataBP) throws MessagingException
    {
        if (logger.isTraceEnabled())
        {
            logger.trace("Adding custom headers to outer MBP...." + msg.getLogMsgID());
        }
        Map<String, String> hdrs = msg.getCustomOuterMimeHeaders();
        if (hdrs == null)
        {
            return;
        }
        for (Map.Entry<String, String> entry : hdrs.entrySet())
        {
            dataBP.addHeader(entry.getKey(), entry.getValue());
            if (logger.isTraceEnabled())
            {
                logger.trace("Added custom headers to outer MBP: " + entry.getKey() + "--->" + entry.getValue() + msg.getLogMsgID());
            }
        }
    }

    protected void LogHttpHeadersInBlob(HttpURLConnection conn, Message msg, MimeBodyPart securedData)
    {
        try {
            logger.info("LogHttpHeadersInBlob 1 ");
            StringBuilder ReqBulider = new StringBuilder();
            String RequestString = "";
            Partnership partnership = msg.getPartnership();


            ReqBulider.append("User-Agent:=" + msg.getAppTitle() + " (AS2Sender)");
            ReqBulider.append("\n");
            // Ensure date is formatted in english so there are only USASCII chars to avoid error

            ReqBulider.append("Date:=" +
                    DateUtil.formatDate(
                            Properties.getProperty("HTTP_HEADER_DATE_FORMAT", "EEE, dd MMM yyyy HH:mm:ss Z")
                            , Locale.ENGLISH));
            ReqBulider.append("\n");

            ReqBulider.append("Message-ID:=" + msg.getMessageID());
            ReqBulider.append("\n");

            ReqBulider.append("Mime-Version:=" + "1.0");
            ReqBulider.append("\n");
            // encoding used in the
            // msg, run TBF1

            ReqBulider.append("Content-type:=" + conn.getContentType());
            ReqBulider.append("\n");
            logger.info("LogHttpHeadersInBlob 2 ");

            logger.info("LogHttpHeadersInBlob 3 ");
            ReqBulider.append("AS2-Version:=" + "1.1");
            ReqBulider.append("\n");
            ReqBulider.append("Cache-Control:=" + conn.getHeaderField("Cache-Control"));
            ReqBulider.append("\n");
            logger.info("LogHttpHeadersInBlob 4 ");
            String cte = null;
            try {
                cte = securedData.getEncoding();
            } catch (MessagingException e1) {
                e1.printStackTrace();
            }
            if (cte == null) {
                cte = Session.DEFAULT_CONTENT_TRANSFER_ENCODING;
            }

            logger.info("LogHttpHeadersInBlob 6 ");
            ReqBulider.append("Content-Transfer-Encoding:=" + cte);
            ReqBulider.append("\n");

            logger.info("LogHttpHeadersInBlob 7 Recipient-Address ");
            ReqBulider.append("Recipient-Address:=" + partnership.getAttribute(AS2Partnership.PA_AS2_URL));
            ReqBulider.append("\n");

            ReqBulider.append("AS2-To:=" + partnership.getReceiverID(AS2Partnership.PID_AS2));
            ReqBulider.append("\n");

            ReqBulider.append("AS2-From:=" + partnership.getSenderID(AS2Partnership.PID_AS2));
            ReqBulider.append("\n");

            ReqBulider.append("Subject:=" + msg.getSubject());
            ReqBulider.append("\n");

            ReqBulider.append("From:=" + partnership.getSenderID(Partnership.PID_EMAIL));
            ReqBulider.append("\n");
            String dispTo = partnership.getAttribute(AS2Partnership.PA_AS2_MDN_TO);

            if (dispTo != null) {

                ReqBulider.append("Disposition-Notification-To:=" + dispTo);
                ReqBulider.append("\n");
            }

            String dispOptions = partnership.getAttribute(AS2Partnership.PA_AS2_MDN_OPTIONS);

            if (dispOptions != null) {

                ReqBulider.append("Disposition-Notification-Options:=" + dispOptions);
                ReqBulider.append("\n");
            }

            String receiptOption = partnership.getAttribute(AS2Partnership.PA_AS2_RECEIPT_OPTION);
            if (receiptOption != null) {

                ReqBulider.append("Receipt-Delivery-Option:=" + receiptOption);
                ReqBulider.append("\n");
            }

            String contentDisp;
            try {
                contentDisp = securedData.getDisposition();
            } catch (MessagingException e) {
                contentDisp = msg.getContentDisposition();
            }
            if (contentDisp != null) {

                ReqBulider.append("Content-Disposition:=" + contentDisp);
                ReqBulider.append("\n");
            }
            if ("true".equalsIgnoreCase((partnership.getAttribute(AS2Partnership.PA_ADD_CUSTOM_MIME_HEADERS_TO_HTTP)))) {
                if (logger.isTraceEnabled()) {
                    logger.trace("Adding custom headers to HTTP..." + msg.getLogMsgID());
                }
                for (Map.Entry<String, String> entry : msg.getCustomOuterMimeHeaders().entrySet()) {

                    ReqBulider.append(entry.getKey() + ":=" + entry.getValue());
                    ReqBulider.append("\n");
                }

            }
            ReqBulider.append("Connection:=close, TE");
            ReqBulider.append("\n");
            ReqBulider.append(securedData.getContentMD5());
            ReqBulider.append("\n");
            RequestString = ReqBulider.toString();
            logger.info(RequestString);
            // Log Request in blob
            BlobHelper blobHelper = new BlobHelper();
            try {
                blobHelper.UploadFileInBlob(msg.getPartnership().getAttribute("blobContainer"), msg.getMessageID() + ".req", RequestString.getBytes());
                logger.info("LogHttpHeadersInBlob 6 upload content in blob "+msg.getMessageID() + ".req in container "+msg.getPartnership().getAttribute("blobContainer") );
            } catch (Exception exp) {
                logger.error(exp);
            }

        }
        catch (Exception exp)
        {
            logger.error(exp);
        }
    }

    protected void updateHttpHeaders(HttpURLConnection conn, Message msg, MimeBodyPart securedData)
    {
        Partnership partnership = msg.getPartnership();

        conn.setRequestProperty("Connection", "close, TE");
        conn.setRequestProperty("User-Agent", msg.getAppTitle() + " (AS2Sender)");

		// Ensure date is formatted in english so there are only USASCII chars to avoid error
        conn.setRequestProperty("Date",
        		DateUtil.formatDate(
        				Properties.getProperty("HTTP_HEADER_DATE_FORMAT", "EEE, dd MMM yyyy HH:mm:ss Z")
        				, Locale.ENGLISH));
        conn.setRequestProperty("Message-ID", msg.getMessageID());
        conn.setRequestProperty("Mime-Version", "1.0"); // make sure this is the
        // encoding used in the
        // msg, run TBF1
        try
        {
            conn.setRequestProperty("Content-type", securedData.getContentType());
        } catch (MessagingException e)
        {
            conn.setRequestProperty("Content-type", msg.getContentType());
        }
        conn.setRequestProperty("AS2-Version", "1.1"); // RFC6017 - AS2 V1.1 supports compression
        // AS2 V1.2 additionally supports EDIINT-Features
        // conn.setRequestProperty("EDIINT-Features",
        // "CEM,multiple-attachments"); // TODO (possibly implement???)
        String cte = null;
        try
        {
            cte = securedData.getEncoding();
        } catch (MessagingException e1)
        {
            e1.printStackTrace();
        }
        if (cte == null)
        {
            cte = Session.DEFAULT_CONTENT_TRANSFER_ENCODING;
        }
        conn.setRequestProperty("Content-Transfer-Encoding", cte);
        conn.setRequestProperty("Recipient-Address", partnership.getAttribute(AS2Partnership.PA_AS2_URL));
        conn.setRequestProperty("AS2-To", partnership.getReceiverID(AS2Partnership.PID_AS2));
        conn.setRequestProperty("AS2-From", partnership.getSenderID(AS2Partnership.PID_AS2));
        conn.setRequestProperty("Subject", msg.getSubject());
        conn.setRequestProperty("From", partnership.getSenderID(Partnership.PID_EMAIL));

        String dispTo = partnership.getAttribute(AS2Partnership.PA_AS2_MDN_TO);

        if (dispTo != null)
        {
            conn.setRequestProperty("Disposition-Notification-To", dispTo);
        }

        String dispOptions = partnership.getAttribute(AS2Partnership.PA_AS2_MDN_OPTIONS);

        if (dispOptions != null)
        {
            conn.setRequestProperty("Disposition-Notification-Options", dispOptions);
        }

        String receiptOption = partnership.getAttribute(AS2Partnership.PA_AS2_RECEIPT_OPTION);
        if (receiptOption != null)
        {
            conn.setRequestProperty("Receipt-Delivery-Option", receiptOption);
        }

        String contentDisp;
        try
        {
            contentDisp = securedData.getDisposition();
        } catch (MessagingException e)
        {
            contentDisp = msg.getContentDisposition();
        }
        if (contentDisp != null)
        {
            conn.setRequestProperty("Content-Disposition", contentDisp);
        }
        if ("true".equalsIgnoreCase((partnership.getAttribute(AS2Partnership.PA_ADD_CUSTOM_MIME_HEADERS_TO_HTTP))))
        {
            if (logger.isTraceEnabled())
            {
                logger.trace("Adding custom headers to HTTP..." + msg.getLogMsgID());
            }
            for (Map.Entry<String, String> entry : msg.getCustomOuterMimeHeaders().entrySet())
            {
                conn.setRequestProperty(entry.getKey(), entry.getValue());
            }
        }

    }

    /**
     * Stores metadata into pending information file and storing
     * message object from first send attempt. The message object
     * is written to a separate file to avoid repeated rewrites of
     * possibly very large objects since it contains the original
     * file data
     * @param msg AS2Message structure
     * @param isResend Boolean to determine if this is a resend of an already sent message or not
     * @throws Exception some unforseen issue has occurred
     *
     */
    protected void storePendingInfo(AS2Message msg, boolean isResend) throws Exception
    {
        ObjectOutputStream oos = null;

        try
        {
            String pendingInfoFile = AS2Util.buildPendingFileName(msg, getSession().getProcessor(), "pendingmdninfo");
            String pendingFile = msg.getAttribute(FileAttribute.MA_PENDINGFILE);
            msg.setAttribute(FileAttribute.MA_PENDINGFILE, pendingFile);
            if (!isResend)
            {
                // Write the object to a file to keep a lot of the original
                // static metadata intact for resends
                String pendingMsgObjFile = pendingFile + ".object";
                oos = new ObjectOutputStream(new FileOutputStream(pendingMsgObjFile));
                oos.writeObject(msg);
                oos.flush();
                oos.close();
            }
            msg.setAttribute(FileAttribute.MA_PENDINGINFO, pendingInfoFile);
            oos = new ObjectOutputStream(new FileOutputStream(pendingInfoFile));
            oos.writeObject(msg.getCalculatedMIC());
            String retries = (String) msg.getOption(ResenderModule.OPTION_RETRIES);
            oos.writeObject((retries == null ? "" : retries));

            if (logger.isInfoEnabled())
            {
                logger.info("Save Original mic & message id information into file: " + pendingInfoFile
                        + msg.getLogMsgID());
            }
            oos.writeObject(msg.getAttribute(FileAttribute.MA_FILENAME));
            oos.writeObject(pendingFile);
            oos.writeObject(msg.getAttribute(FileAttribute.MA_ERROR_DIR));
            String sentDir = msg.getAttribute(FileAttribute.MA_SENT_DIR);
            oos.writeObject((sentDir == null ? "" : sentDir));
            oos.writeObject(msg.getAttributes());
            if (logger.isTraceEnabled())
            {
                logger.trace("Pending info file written to:" + pendingInfoFile + "\n\tOriginal MIC: "
                        + msg.getCalculatedMIC() + "\n\tRetry Count: " + retries
                        + "\n\tOriginal file name : " + msg.getAttribute(FileAttribute.MA_FILENAME)
                        + "\n\tPending message file : " + pendingFile + "\n\tError directory: "
                        + msg.getAttribute(FileAttribute.MA_ERROR_DIR) + "\n\tSent directory: "
                        + msg.getAttribute(FileAttribute.MA_SENT_DIR) + msg.getLogMsgID());
            }

            msg.setAttribute(FileAttribute.MA_STATUS, FileAttribute.MA_PENDING);

        } catch (Exception e)
        {
            msg.setLogMsg("Error setting up pending information files: " + org.openas2.logging.Log.getExceptionMsg(e));
            logger.error(msg, e);
            throw new Exception("Unable to set up pending information files.");
        } finally
        {
            if (oos != null)
            {
                try
                {
                    oos.close();
                } catch (IOException e)
                {
                }
            }
        }
    }

    protected void calcAndStoreMic(Message msg, MimeBodyPart mbp, boolean includeHeaders) throws Exception
    {
        // Calculate and get the original mic
        // includeHeaders = (msg.getHistory().getItems().size() > 1);

        DispositionOptions dispOptions = new DispositionOptions(msg.getPartnership().getAttribute(
                AS2Partnership.PA_AS2_MDN_OPTIONS));
        msg.setCalculatedMIC(AS2Util.getCryptoHelper().calculateMIC(mbp, dispOptions.getMicalg()
                , includeHeaders, msg.getPartnership().isPreventCanonicalization()));
        if (logger.isTraceEnabled())
        {
            // Generate some alternative MIC's to see if the partner is somehow using a different default
            String tmic = AS2Util.getCryptoHelper().calculateMIC(mbp, dispOptions.getMicalg()
                    , includeHeaders, !msg.getPartnership().isPreventCanonicalization());
            logger.trace("MIC outbound with forced reversed prevent canocalization: " + tmic + msg.getLogMsgID());
            tmic = AS2Util.getCryptoHelper().calculateMIC(msg.getData(), dispOptions.getMicalg(),
                    false, msg.getPartnership().isPreventCanonicalization());
            logger.trace("MIC outbound with forced exclude headers flag: " + tmic + msg.getLogMsgID());

        }
    }
    
    protected byte[] toByteArrayInputStream(InputStream is, Message msg) throws Exception
    {
    	try {

    
    		//byte[] result = IOUtils.toByteArray(is);
    		
    		ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    	    int nRead;
    	    byte[] data = new byte[1024];
    	    logger.info("Converting MDN InputStream To ByteArray Stage 9.0.2" + msg.getLogMsgID()+ " - Available InputStream Bytes"+ is.available());
    	  
    	    boolean completeFlg = false;
    	   
    	    while (completeFlg != true &&(nRead = is.read(data, 0, data.length)) != -1) 
    	    {
    	    	logger.info("Converting MDN InputStream To ByteArray Stage 9.0.2.0" + msg.getLogMsgID()+ " - Available InputStream Bytes"+ is.available());
    	        buffer.write(data, 0, nRead);
    	        
    	        int bytesRemain = is.available();
    	        int retryCntr = 1;
    	        int numberOfRtrys = 72;
    	        
    	        //Test the data we received to see if we have completed the MDN transport of data on the Stream
    	        byte[] tstresult = buffer.toByteArray();
    	        boolean fileCmplt = AS2Util.testStreamforCompleteData((AS2Message) msg, tstresult, getSession());
    	        logger.info("Testing Data From Stream Stage 9.0.3.0" + msg.getLogMsgID()+ " - Test: "+ fileCmplt);
    	        
    	       // 72 retrys X 2.5 second waits = 3 minute total wait time for a thread trying to read inputstream data
    	        while (!fileCmplt && bytesRemain == 0 && retryCntr <= numberOfRtrys)
    	        {	Thread.sleep(2500);// 2.5 seconds
    	            logger.info("Retrying to Read MDN Data From Stream Stage 9.0.4.0" + msg.getLogMsgID()+ " - Retry Count"+ retryCntr);
    	        	retryCntr ++;   	        
    	            bytesRemain = is.available();
    	            
    	        	if (bytesRemain == 0 && retryCntr > numberOfRtrys)
    	        		completeFlg = true;
    	        }
    	    }
    	    
    	    buffer.flush();
    	    byte[] result = buffer.toByteArray();
    		
    		is.close();
    		is = null;
    		
    		
     		return result;
    		
    	} catch (Exception e) 
    	{
    		logger.info("MDN ByteArray Conversion Failed..." + msg.getLogMsgID() + " - "+e.getMessage());
    		throw new Exception(e);
    	}finally
    	{
            if (is != null)
            {
                is.close();
                is = null;
            }

    	}
    }
    
    
}
