package org.openas2.processor.receiver;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.HashMap;

import javax.activation.DataHandler;
import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.internet.ContentType;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.ParseException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.openas2.DispositionException;
import org.openas2.OpenAS2Exception;
import org.openas2.Session;
import org.openas2.WrappedException;
import org.openas2.cert.CertificateFactory;
import org.openas2.lib.helper.ICryptoHelper;
import org.openas2.lib.util.MimeUtil;
import org.openas2.message.AS2Message;
import org.openas2.message.Message;
import org.openas2.message.MessageMDN;
import org.openas2.message.NetAttribute;
import org.openas2.partner.AS2Partnership;
import org.openas2.partner.ASXPartnership;
import org.openas2.partner.Partnership;
import org.openas2.processor.sender.SenderModule;
import org.openas2.processor.storage.StorageModule;
import org.openas2.util.*;
import org.openas2.util.Properties;

import java.util.*;

public class AS2ReceiverHandler implements NetModuleHandler {
    private AS2ReceiverModule module;
    private boolean removeHeaderFolding;

	private Log logger = LogFactory.getLog(AS2ReceiverHandler.class.getSimpleName());

    
    public AS2ReceiverHandler(AS2ReceiverModule module) {
        super();
        this.module = module;
        removeHeaderFolding = "true".equals(Properties.getProperty("remove_http_header_folding", "true"));
    }

    public String getClientInfo(Socket s) {
        return " " + s.getInetAddress().getHostAddress() + " " + Integer.toString(s.getPort());
    }

    public AS2ReceiverModule getModule() {
        return module;
    }

    public void handle(NetModule owner, Socket s) {

        if (logger.isDebugEnabled()) logger.debug("incoming connection"+getClientInfo(s));



        AS2Message msg = createMessage(s);

        byte[] data = null;
        BufferedOutputStream out;

		msg.setOption("DIRECTION", "RECEIVE");

		try {
			out = new BufferedOutputStream(s.getOutputStream());
		} catch (IOException e1) {
			msg.setLogMsg("Failed to get outputstream on received socket. Response cannot be sent.");
			System.out.println("Failed to get outputstream on received socket. Response cannot be sent."+ e1.getMessage());
			logger.error(msg, e1);
			return;
		}

        try {
			// Time the transmission
			ProfilerStub transferStub = Profiler.startProfile();
			// Read in the message request, headers, and data
			try {
				data = HTTPUtil.readData(s.getInputStream(), s.getOutputStream(), msg);

			} catch (Exception e) {
				msg.setLogMsg("HTTP connection error on inbound message.");
				System.out.println("HTTP connection error on inbound message."+e.getMessage());
				logger.error(msg, e);
				NetException ne = new NetException(s.getInetAddress(), s.getPort(), e);
				ne.terminate();
			}
			Profiler.endProfile(transferStub);
            msg.setPayloadFilename(msg.getHeader("AS2-From"));
			String mic = null;
			if (data == null)
			{
				if ("true".equalsIgnoreCase(msg.getAttribute("isHealthCheck")))
				{
					msg.setLogMsg("");
				    if (logger.isInfoEnabled())

						logger.info("Healthcheck ping detected" + " [" + getClientInfo(s) + "]"
								+ msg.getLogMsgID());
					return;
				}
				else
				{
					try
					{
						HTTPUtil.sendHTTPResponse(s.getOutputStream(), HttpURLConnection.HTTP_BAD_REQUEST, false);
					} catch (IOException e1)
					{
					}
					OpenAS2Exception oe = new OpenAS2Exception("Missing data in AS2 request.");
					msg.setLogMsg("Error receiving message for inbound AS2 request. There is no data.");
					System.out.println("Error receiving message for inbound AS2 request. There is no data.");
					return;
				}
			}
			else
			{
				if (logger.isDebugEnabled())
				        msg.setLogMsg("received " + IOUtilOld.getTransferRate(data.length, transferStub) + getClientInfo(s)
                                + msg.getLogMsgID());
						logger.debug(msg);

				if (logger.isTraceEnabled())
				{
					logger.trace("Received msg built from HTTP input stream: " + msg.toString() + msg.getLogMsgID());
				}
				// TODO store HTTP request, headers, and data to file in Received folder -> use message-id for filename?
				try {
					// Put received data in a MIME body part
					ContentType receivedContentType = null;

					try {
						/*
						 * receivedPart = new MimeBodyPart(msg.getHeaders(), data); msg.setData(receivedPart);
						 * receivedContentType = new ContentType(receivedPart.getContentType());
						 */
						receivedContentType = new ContentType(msg.getHeader("Content-Type"));

						MimeBodyPart receivedPart = new MimeBodyPart();
						receivedPart.setDataHandler(
								new DataHandler(new ByteArrayDataSource(data, receivedContentType.toString(), null)));
						if (logger.isTraceEnabled() && "true".equalsIgnoreCase(System.getProperty("logRxdMsgMimeBodyParts", "false")))
						{
							logger.trace("Received MimeBodyPart for inbound message: " + msg.getLogMsgID()
									+ "\n" + MimeUtil.toString(receivedPart, true));
						}
						// Set "Content-Type" and "Content-Transfer-Encoding" to what is received in the HTTP header
						// since it may not be set in the received mime body part
						receivedPart.setHeader("Content-Type", receivedContentType.toString());
						
						// Set the transfer encoding if necessary
						String cte =  receivedPart.getEncoding();
						if (cte == null)
					    {
							// Not in the MimeBodyPart so try the HTTP headers...
							cte = msg.getHeader("Content-Transfer-Encoding");
							// Nada ... set to system default
							if (cte == null) cte = Session.DEFAULT_CONTENT_TRANSFER_ENCODING;
							receivedPart.setHeader("Content-Transfer-Encoding", cte);
					    }
						else if (logger.isTraceEnabled())
						{
							logger.trace("Received msg MimePart has transfer encoding: " + cte + msg.getLogMsgID());
						}
						msg.setData(receivedPart);
					} catch (Exception e) {
						msg.setLogMsg("Error extracting received message.");
						System.out.println("Error extracting received message.");
						logger.error(msg, e);
						throw new DispositionException(new DispositionType("automatic-action", "MDN-sent-automatically",
								"processed", "Error", "unexpected-processing-error"),
								AS2ReceiverModule.DISP_PARSING_MIME_FAILED, e);
					}

					// Check if request is forwarded by a reverse proxy
					String sourceIpAddress = msg.getXForwardedFor();
					if (sourceIpAddress == null) {
						sourceIpAddress = msg.getXRealIP();
					}
					if(sourceIpAddress != null) {
					    msg.setLogMsg(msg.getLogMsgID() + " AS2 message has been forwarded by the proxy " + msg.getAttribute(NetAttribute.MA_SOURCE_IP) + ", the original server IP address is " + sourceIpAddress);
						logger.info(msg);
						msg.setAttribute(NetAttribute.MA_SOURCE_IP, sourceIpAddress);
					}

					// Extract AS2 ID's from header, find the message's partnership and update the message
					try {
						msg.getPartnership().setSenderID(AS2Partnership.PID_AS2, msg.getHeader("AS2-From"));
						msg.getPartnership().setReceiverID(AS2Partnership.PID_AS2, msg.getHeader("AS2-To"));

						getModule().getSession().getPartnershipFactory().updatePartnership(msg, false);
					} catch (OpenAS2Exception oae) {
						System.out.println(AS2ReceiverModule.DISP_PARTNERSHIP_NOT_FOUND);
						throw new DispositionException(new DispositionType("automatic-action", "MDN-sent-automatically",
								"processed", "Error", "authentication-failed"),
								AS2ReceiverModule.DISP_PARTNERSHIP_NOT_FOUND, oae);

					}
					// Log significant msg state
					msg.setOption("STATE", Message.MSG_STATE_RECEIVE_START);
					msg.trackMsgState(getModule().getSession());
					// Decrypt and verify signature of the data, and attach data to the message
					mic = decryptAndVerify(msg);
					try
					{
						// Extract and Store the received filename of the payload
						msg.setPayloadFilename(msg.extractPayloadFilename());
					} catch (ParseException e1)
					{
						System.out.println("Failed to extract the file name from received content-disposition");
						logger.error("Failed to extract the file name from received content-disposition", e1);
					}


					try {
						Map<Object,Object> optMap =new HashMap<Object,Object>();
						optMap.put("queueName",msg.getPartnership().getAttribute("Inqueue"));
						optMap.put("blobContainer",msg.getPartnership().getAttribute("blobContainer"));
						optMap.put("MaxFileSize_Queue",msg.getPartnership().getAttribute("MaxFileSize_Queue"));
						getModule().getSession().getProcessor().handle(StorageModule.DO_STORE, msg,optMap);
                        msg.setLogMsg("Received File Saved in "+msg.getHeader("AS2-From")+"queue" );
                        logger.info(msg);
					} catch (OpenAS2Exception oae) {
						msg.setLogMsg("Error handling received message: " + oae.getCause());
						System.out.println("Error handling received message: " + oae.getCause());
						logger.error(msg, oae);
						// Log significant msg state
						msg.setOption("STATE", Message.MSG_STATE_RECEIVE_EXCEPTION);
						msg.trackMsgState(getModule().getSession());

						throw new DispositionException(new DispositionType("automatic-action", "MDN-sent-automatically",
								"processed", "Error", "unexpected-processing-error"),
								AS2ReceiverModule.DISP_STORAGE_FAILED, oae);
					}
					// Put the file inthe queue
					// Transmit a success MDN if requested
					try {
						if (msg.isRequestingMDN()) {
							// Log significant msg state
							msg.setOption("STATE", Message.MSG_STATE_MDN_SEND_START);
							msg.trackMsgState(getModule().getSession());
							sendMDNResponse(msg, out,
									new DispositionType("automatic-action", "MDN-sent-automatically", "processed"), mic,
									AS2ReceiverModule.DISP_SUCCESS);
			                //if asyncMDN requested, close connection and initiate separate MDN send 
			                if (msg.isRequestingAsynchMDN() ) {
			                	out.close();
								out = null; // Prevent yet another error in finally block
								Map<Object,Object> optMap =new HashMap<Object,Object>();
								optMap.put("blobContainer",msg.getPartnership().getAttribute("blobContainer"));
			                    getModule().getSession().getProcessor().handle(SenderModule.DO_SENDMDN, msg, optMap);
			                	if (logger.isDebugEnabled())
				                	  logger.debug("Call to asynch MDN initiated");
			                    return;
			                }
							// Log significant msg state
							//msg.setOption("STATE", Message.MSG_STATE_MSG_SENT_MDN_RECEIVED_OK);
							//msg.setOption("STATE", Message.MSG_STATE_MSG_RXD_MDN_SENT_OK);
							msg.setStatus(Message.MSG_STATE_MSG_RXD_MDN_SENT_OK);
			                msg.setLogMsg("Message received and MDN sent successfully.");
							msg.trackMsgState(getModule().getSession());

							logger.info(msg);

						} else {
							HTTPUtil.sendHTTPResponse(out, HttpURLConnection.HTTP_OK, false);
							out.flush();
							msg.setLogMsg("Sent HTTP OK"+"((msg.getPayloadFilename().trim()!=\"\")?\"[For FileName=\"+msg.getPayloadFilename()+\"]\":\"\")"+ getClientInfo(s) + msg.getLogMsgID());
							logger.info(msg);
						}

					} catch (Exception e) {
						msg.setLogMsg("Error processing MDN for received message: " + e.getCause());
						System.out.println("Error processing MDN for received message: " + e.getCause());
						logger.error(msg, e);
						// Log significant msg state
						msg.setOption("STATE", Message.MSG_STATE_MDN_SENDING_EXCEPTION);
						msg.trackMsgState(getModule().getSession());
						throw new WrappedException("Error creating and returning MDN, message was still processed",
								e);
					}

				} catch (DispositionException de) {
					// Log significant msg state
					msg.setOption("STATE", Message.MSG_STATE_MSG_SENT_MDN_RECEIVED_ERROR);
					System.out.println("STATE"+ Message.MSG_STATE_MSG_SENT_MDN_RECEIVED_ERROR);
					msg.trackMsgState(getModule().getSession());
					sendMDNResponse(msg, out, de.getDisposition(), mic, de.getText());
	                //if asyncMDN requested, close connection and initiate separate MDN send 
	                if (msg.isRequestingAsynchMDN() ) {
	                	try {
							out.close();
							out = null; // Prevent yet another error in finally block
						} catch (IOException e) {
							msg.setLogMsg("Failed to close connection on DispositionException handling to send async MDN.");
							System.out.println("Failed to close connection on DispositionException handling to send async MDN.");
							logger.error(msg, e);
						}
	                    try {
							Map<Object,Object> optMap =new HashMap<Object,Object>();
							optMap.put("blobContainer",msg.getPartnership().getAttribute("blobContainer"));
							getModule().getSession().getProcessor().handle(SenderModule.DO_SENDMDN, msg, optMap);
		                	if (logger.isDebugEnabled())
			                	  logger.debug("Call to asynch MDN sender initiated");
						} catch (Exception e) {
							msg.setLogMsg("Failed to initiate async MDN send on DispositionException handling.");
							System.out.println("Failed to close connection on DispositionException handling to send async MDN.");
							logger.error(msg, e);
							// Log significant msg state
							msg.setOption("STATE", Message.MSG_STATE_MSG_RXD_MDN_SENDING_FAIL);
							msg.trackMsgState(getModule().getSession());
						}
	                    return;
	                }

					getModule().handleError(msg, de);
				} catch (OpenAS2Exception oae) {
					// Log significant msg state
					msg.setOption("STATE", Message.MSG_STATE_RECEIVE_FAIL);
					System.out.println("STATE"+ Message.MSG_STATE_RECEIVE_FAIL);
					msg.trackMsgState(getModule().getSession());
					getModule().handleError(msg, oae);
				}


			}

		} finally {

			// Process the received message
			if (!( "true".equalsIgnoreCase(msg.getAttribute("isHealthCheck")))) {

				LogHttpHeadersInBlob(msg, data);
			}
			if (out != null)
			{
				try {
					out.close();
				} catch (IOException e) {
					msg.setLogMsg("Failed to close output connection.");
					System.out.println("Failed to close output connection.");
					logger.error(msg, e);;
				}
			}
		}
    }



	protected void LogHttpHeadersInBlob(Message msg,byte[] Data)
	{
		try {
			if(logger.isDebugEnabled())
			logger.debug("Log Http Headers In Blob during receiving file ");
			StringBuilder ReqBulider = new StringBuilder();
			Partnership partnership = msg.getPartnership();
			ReqBulider.append("In-Coming Request Details");
			ReqBulider.append("\n");

            ReqBulider.append("User-Agent:=" + msg.getAppTitle() + " (AS2Sender)");
            ReqBulider.append("\n");
            // Ensure date is formatted in english so there are only USASCII chars to avoid error
			ReqBulider.append("Date:=" + DateTime.now().toString("dd MMM yyyy HH:mm:ss Z",Locale.ENGLISH));
           /* ReqBulider.append("Date:=" +
                    DateUtil.formatDate(
                            Properties.getProperty("HTTP_HEADER_DATE_FORMAT", "EEE, dd MMM yyyy HH:mm:ss Z")
                            , Locale.ENGLISH));*/
            ReqBulider.append("\n");
			// encoding used in the
			// msg, run TBF1
			//ReqBulider.append("Content-Transfer-Encoding:=" + msg.getHeader("Content-Transfer-Encoding"));
			//ReqBulider.append("\n");


			ReqBulider.append("Recipient-Address:=" + partnership.getAttribute(AS2Partnership.PA_AS2_URL));
			ReqBulider.append("\n");
            ReqBulider.append("Sender-Address:=" + msg.getAttribute("HTTP_REQUEST_URL"));
            ReqBulider.append("\n");
            ReqBulider.append("Request-Type:=" + msg.getAttribute("MA_HTTP_REQ_TYPE"));
            ReqBulider.append("\n");



			//ReqBulider.append("AS2-To:=" + partnership.getReceiverID(AS2Partnership.PID_AS2));
			//ReqBulider.append("\n");

			//ReqBulider.append("AS2-From:=" + partnership.getSenderID(AS2Partnership.PID_AS2));
			//ReqBulider.append("\n");

			ReqBulider.append("Subject:=" + msg.getSubject());
			ReqBulider.append("\n");

			ReqBulider.append("From:=" + partnership.getSenderID(Partnership.PID_EMAIL));
			ReqBulider.append("\n");
			//String dispTo = partnership.getAttribute(AS2Partnership.PA_AS2_MDN_TO);

			//if (dispTo != null) {

			//	ReqBulider.append("Disposition-Notification-To:=" + dispTo);
			//	ReqBulider.append("\n");
			//}

			//String dispOptions = partnership.getAttribute(AS2Partnership.PA_AS2_MDN_OPTIONS);

			//if (dispOptions != null) {

			//	ReqBulider.append("Disposition-Notification-Options:=" + dispOptions);
			//	ReqBulider.append("\n");
			//}

			String receiptOption = partnership.getAttribute(AS2Partnership.PA_AS2_RECEIPT_OPTION);
			if (receiptOption != null) {

				ReqBulider.append("Receipt-Delivery-Option:=" + receiptOption);
				ReqBulider.append("\n");
			}

				//ReqBulider.append("Content-Disposition:=" + msg.getHeader("Content-Disposition"));
				///ReqBulider.append("\n");

           // ReqBulider.append("Content-Length:=" + msg.getHeader("Content-Length"));
            //ReqBulider.append("\n");

			if ("true".equalsIgnoreCase((partnership.getAttribute(AS2Partnership.PA_ADD_CUSTOM_MIME_HEADERS_TO_HTTP)))) {
				if (logger.isTraceEnabled()) {
					logger.trace("Adding custom headers to HTTP..." + msg.getLogMsgID());
				}
				for (Map.Entry<String, String> entry : msg.getCustomOuterMimeHeaders().entrySet()) {

					ReqBulider.append(entry.getKey() + ":=" + entry.getValue());
					ReqBulider.append("\n");
				}

			}
            ReqBulider.append("\n");
            ReqBulider.append(HTTPUtil.printHeaders(msg.getHeaders().getAllHeaders()));
            ReqBulider.append("\n");
			ReqBulider.append("Connection:="+msg.getHeader("Connection"));
			ReqBulider.append("\n");
			ReqBulider.append("Data");
			ReqBulider.append("\n");
			ReqBulider.append(HTTPUtil.getBody(msg.getData().getInputStream()));
			ReqBulider.append("\n");


			// Log Request in blob
			BlobHelper blobHelper = new BlobHelper();
			try {
				blobHelper.UploadFileInBlob(msg.getPartnership().getAttribute("blobContainer"), msg.getMessageID().replace("<","").replace(">","").trim() + ".req", ReqBulider.toString().getBytes());
				if(logger.isDebugEnabled())
					logger.debug("LogHttpHeadersInBlob 6 upload content in blob "+msg.getMessageID().replace("<","").replace(">","").trim() + ".req in container "+msg.getPartnership().getAttribute("blobContainer") );
			} catch (Exception exp) {
				logger.error(exp);
			}

		}
		catch (Exception exp)
		{
			logger.error(exp);
		}
	}




    // Create a new message and record the source ip and port
    protected AS2Message  createMessage(Socket s) {
        AS2Message msg = new AS2Message();

        msg.setAttribute(NetAttribute.MA_SOURCE_IP, s.getInetAddress().toString());
        msg.setAttribute(NetAttribute.MA_SOURCE_PORT, Integer.toString(s.getPort()));
        msg.setAttribute(NetAttribute.MA_DESTINATION_IP, s.getLocalAddress().toString());
        msg.setAttribute(NetAttribute.MA_DESTINATION_PORT, Integer.toString(s.getLocalPort()));

        return msg;
    }

    protected String decryptAndVerify(AS2Message msg) throws OpenAS2Exception {
        CertificateFactory certFx = getModule().getSession().getCertificateFactory();
        ICryptoHelper ch;
        String mic = null;

        try {
            ch = AS2Util.getCryptoHelper();
        } catch (Exception e) {
            throw new WrappedException(e);
        }
		// Per RFC5402 compression is always before encryption but can be before or after signing of message but only in one place
        boolean isDecompressed = false;
        
        try
        {
            if (ch.isEncrypted(msg.getData())) {
            	msg.setRxdMsgWasEncrypted(true);
              // Decrypt
            	if (logger.isDebugEnabled()) logger.debug("decrypting :::"+msg.getLogMsgID());

                X509Certificate receiverCert = certFx.getCertificate(msg, Partnership.PTYPE_RECEIVER);
                PrivateKey receiverKey = certFx.getPrivateKey(msg, receiverCert);
                msg.setData(AS2Util.getCryptoHelper().decrypt(msg.getData(), receiverCert, receiverKey));
				if (logger.isTraceEnabled() && "true".equalsIgnoreCase(System.getProperty("logRxdMsgMimeBodyParts", "false")))
				{
					logger.trace("Received MimeBodyPart for inbound message after decryption: " + msg.getLogMsgID()
							+ "\n" + MimeUtil.toString(msg.getData(), true));
				}
            }
        } catch (Exception e) {
        	msg.setLogMsg("Error extracting received message: " + e.getCause());
			System.out.println("Error extracting received message: " + e.getCause());
        	logger.error(msg, e);;
            throw new DispositionException(new DispositionType("automatic-action", "MDN-sent-automatically",
                    "processed", "Error", "decryption-failed"), AS2ReceiverModule.DISP_DECRYPTION_ERROR, e);
        }

		try {
			if (ch.isCompressed(msg.getData()))
			{
				if (logger.isTraceEnabled()) logger.trace("Decompressing received message before checking signature...");
				AS2Util.getCryptoHelper().decompress(msg);
				isDecompressed = true;
				if (logger.isTraceEnabled() && "true".equalsIgnoreCase(System.getProperty("logRxdMsgMimeBodyParts", "false")))
				{
					logger.trace("Received MimeBodyPart for inbound message after decompression: " + msg.getLogMsgID()
							+ "\n" + MimeUtil.toString(msg.getData(), true));
				}
			}
		} catch (Exception e1) {
        	msg.setLogMsg("Error decompressing received message: " + e1.getCause());
			System.out.println("Error decompressing received message: " + e1.getCause());
        	logger.error(msg, e1);;
            throw new DispositionException(new DispositionType("automatic-action", "MDN-sent-automatically",
                    "processed", "Error", "decompresion-failed"), AS2ReceiverModule.DISP_DECOMPRESSION_ERROR, e1);
		}

        try {
            if (ch.isSigned(msg.getData())) {
            	msg.setRxdMsgWasSigned(true);
            	if (logger.isDebugEnabled()) logger.debug("verifying signature"+msg.getLogMsgID());

                X509Certificate senderCert = certFx.getCertificate(msg, Partnership.PTYPE_SENDER);
                msg.setData(AS2Util.getCryptoHelper().verifySignature(msg.getData(), senderCert));
				if (logger.isTraceEnabled() && "true".equalsIgnoreCase(System.getProperty("logRxdMsgMimeBodyParts", "false")))
				{
					logger.trace("Received MimeBodyPart for inbound message after signature verification: " + msg.getLogMsgID()
							+ "\n" + MimeUtil.toString(msg.getData(), true));
				}
            }
        } catch (Exception e) {
        	msg.setLogMsg("Error decrypting received message: " + org.openas2.logging.Log.getExceptionMsg(e));
			System.out.println("Error decrypting received message: " + org.openas2.logging.Log.getExceptionMsg(e));
        	logger.error(msg, e);
            throw new DispositionException(new DispositionType("automatic-action", "MDN-sent-automatically",
                    "processed", "Error", "integrity-check-failed"), AS2ReceiverModule.DISP_VERIFY_SIGNATURE_FAILED, e);
        }

		if (logger.isTraceEnabled())
			try
			{
				logger.trace("SMIME Decrypted Content-Disposition: " + msg.getContentDisposition()
				        + "\n      Content-Type received: " + msg.getContentType()
						+ "\n      HEADERS after decryption: " + msg.getData().getAllHeaders()
				        + "\n      Content-Disposition in MSG getData() MIMEPART after decryption: "
						+ msg.getData().getContentType());
			} catch (MessagingException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

        /*  Calculate the MIC after signing or encryption of the message but prior to doing any decompression
        *   but include headers for unsigned messages (see RFC4130 section 7.3.1 for details)
        */
		DispositionOptions dispOptions = new DispositionOptions(msg
                .getHeader("Disposition-Notification-Options"));
        if (dispOptions.getMicalg() != null) {
            try {
				mic = ch.calculateMIC(msg.getData(), dispOptions.getMicalg(),
				        (msg.isRxdMsgWasSigned() || msg.isRxdMsgWasEncrypted()), msg.getPartnership().isPreventCanonicalization());
		        if (logger.isDebugEnabled()) logger.debug("Prevent Canonicalization: " + msg.getPartnership().isPreventCanonicalization() + " ::: MIC calc on rxd msg: " + mic);
		        if (logger.isTraceEnabled())
		        {
		        	// Generate some alternative MIC's to see if the partner is somehow using a different default
		        	String tmic = ch.calculateMIC(msg.getData(), dispOptions.getMicalg(),
						        (msg.isRxdMsgWasSigned() || msg.isRxdMsgWasEncrypted()), !msg.getPartnership().isPreventCanonicalization());
		        		logger.trace("MIC with forced reversed prevent canocalization: " + tmic + msg.getLogMsgID());
	        		tmic = ch.calculateMIC(msg.getData(), dispOptions.getMicalg(),
						        false, msg.getPartnership().isPreventCanonicalization());
		        		logger.trace("MIC with forced exclude headers flag: " + tmic + msg.getLogMsgID());
		        	
		        }
			} catch (Exception e) {
				msg.setLogMsg("Error calculating MIC on received message: " + e.getCause());
				System.out.println("Error calculating MIC on received message: " + e.getCause());
				logger.error(msg, e);
				throw new DispositionException(new DispositionType("automatic-action", "MDN-sent-automatically",
						"processed", "Error", "unexpected-processing-error"), AS2ReceiverModule.DISP_CALC_MIC_FAILED,
						e);
			}
        }

		// Per RFC5402 compression is always before encryption but can be before or after signing of message but only in one place
		try {
			if (ch.isCompressed(msg.getData()))
			{
				if (isDecompressed)
				{
			        throw new DispositionException(new DispositionType("automatic-action", "MDN-sent-automatically",
			                "processed", "Error", "decompression-failed"), AS2ReceiverModule.DISP_DECOMPRESSION_ERROR
			                , new Exception("Message has already been decompressed. Per RFC5402 it cannot occur twice."));
				}
				if (logger.isTraceEnabled()) logger.trace("Decompressing received message after decryption...");
				AS2Util.getCryptoHelper().decompress(msg);
			}
		} catch (Exception e) {
			msg.setLogMsg("Unexepcted error checking for compressed message after signing");
			System.out.println("Unexepcted error checking for compressed message after signing");
			logger.error(msg, e);
	        throw new DispositionException(new DispositionType("automatic-action", "MDN-sent-automatically",
	                "processed", "Error", "decompression-failed"), AS2ReceiverModule.DISP_DECOMPRESSION_ERROR
	                , new Exception("Unexpected error occurred checking for compressed message: " + e.getMessage()));
		}
		return mic;
    }

    protected void sendMDNResponse(AS2Message msg, BufferedOutputStream out, DispositionType disposition, String mic, String text) {
        boolean mdnBlocked = false;

        mdnBlocked = (msg.getPartnership().getAttribute(ASXPartnership.PA_BLOCK_ERROR_MDN) != null);

        if (!mdnBlocked) {
            try {
            	
            	MessageMDN mdn = AS2Util.createMDN(getModule().getSession(), msg, mic, disposition, text);

                //if asyncMDN requested... 
                if (msg.isRequestingAsynchMDN() ) {
                    HTTPUtil.sendHTTPResponse(out, HttpURLConnection.HTTP_OK, false);
                	out.write("Content-Length: 0\r\n\r\n".getBytes()); 
                	out.flush();
                	if (logger.isInfoEnabled())
                	    msg.setLogMsg("setup to send asynch MDN [" + disposition.toString() + "]"+"For File="+msg.getPayloadFilename()+"MessageId=" + msg.getLogMsgID());
	                	  logger.info(msg);
                    return;
                }
                
                //  otherwise, send sync MDN back on same connection
                HTTPUtil.sendHTTPResponse(out, HttpURLConnection.HTTP_OK, true);


                // make sure to set the content-length header
                ByteArrayOutputStream data = new ByteArrayOutputStream();
                MimeBodyPart part = mdn.getData();
				IOUtils.copy(part.getInputStream(), data);
				mdn.setHeader("Content-Length", Integer.toString(data.size()));


                Enumeration<String> headers = mdn.getHeaders().getAllHeaderLines();
                String header;
                StringBuffer saveHeaders = new StringBuffer();

                while (headers.hasMoreElements()) {
                    header = (String) headers.nextElement();
                    // Support https://tools.ietf.org/html/draft-ietf-httpbis-p1-messaging-13#section-3.2
                    saveHeaders = saveHeaders.append(";;").append(header);
                    if (removeHeaderFolding)
                    	header = header.replaceAll("\r\n[ \t]*", " ");
                    out.write((header + "\r\n").getBytes());
                }

            	if (logger.isTraceEnabled())
              	  logger.trace("MDN HEADERS SENT: " + saveHeaders + msg.getLogMsgID());
                out.write("\r\n".getBytes()); 
                data.writeTo(out);
				out.flush();
                // Save sent MDN  for later examination
				Map<Object,Object> optMap =new HashMap<Object,Object>();
				optMap.put("blobContainer",msg.getPartnership().getAttribute("blobContainer"));
				getModule().getSession().getProcessor().handle(StorageModule.DO_STOREMDN, msg, optMap);
				if (logger.isInfoEnabled()) 
					//logger.info("sent MDN [" + disposition.toString() + "]" + msg.getLogMsgID());

					msg.setLogMsg("sent MDN [" + disposition.toString() + "]" +"For File="+msg.getPayloadFilename()+"MessageId=" + msg.getLogMsgID());
				    logger.info(msg);
            } catch (Exception e) {
                WrappedException we = new WrappedException("Error sending MDN", e);
				System.out.println("Error sending MDN "+ e.getMessage());
                we.addSource(OpenAS2Exception.SOURCE_MESSAGE, msg);
                we.terminate();
                msg.setLogMsg("Unexpected error occurred sending MDN: " + org.openas2.logging.Log.getExceptionMsg(e));
                logger.error(msg, e);
            }
        }
    }
    

 
}
