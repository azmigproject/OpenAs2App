package org.openas2.processor.receiver;
import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import javax.net.ssl.*;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openas2.Constants;
import org.openas2.OpenAS2Exception;
import org.openas2.Session;
import org.openas2.WrappedException;
import org.openas2.message.InvalidMessageException;
import org.openas2.message.Message;
import org.openas2.params.CompositeParameters;
import org.openas2.params.DateParameters;
import org.openas2.params.InvalidParameterException;
import org.openas2.params.MessageParameters;
import org.openas2.util.HTTPUtil;
import org.openas2.util.IOUtilOld;
import org.openas2.util.Properties;

import static org.apache.commons.lang3.StringUtils.defaultIfBlank;


public abstract class NetModule extends BaseReceiverModule {
    public static final String PARAM_ADDRESS = "address";
    public static final String PARAM_PORT = "port";
    public static final String PARAM_PROTOCOL = "protocol";
    public static final String PARAM_SSL_KEYSTORE = "ssl_keystore";
    public static final String PARAM_SSL_KEYSTORE_PASSWORD = "ssl_keystore_password";
    public static final String PARAM_SSL_PROTOCOL = "ssl_protocol";
    public static final String PARAM_ERROR_DIRECTORY = "errordir";
    public static final String PARAM_ERRORS = "errors";
    public static final String DEFAULT_ERRORS = "$date.yyyyMMddhhmmss$";

    private HTTPServerThread mainThread;
    private Log logger = LogFactory.getLog(NetModule.class.getSimpleName());

    public void doStart() throws OpenAS2Exception
    {
        try
        {
            mainThread = new HTTPServerThread(this, getParameter(PARAM_ADDRESS, false), getParameterInt(PARAM_PORT, true));
            mainThread.start();
        } catch (IOException ioe)
        {
            String host = getParameter(PARAM_ADDRESS, false);
            if (host == null || host.length() < 1)
            {
                host = "localhost";
            }
            logger.error("Error in HTTP connection starting server thread on host::port: "
                    + host + "::"
                    + getParameterInt(PARAM_PORT, true), ioe);
            throw new WrappedException(ioe);
        }
    }

    public void doStop() throws OpenAS2Exception
    {
        if (mainThread != null)
        {
            mainThread.terminate();
            mainThread = null;
        }
    }

    public void init(Session session, Map<String, String> options) throws OpenAS2Exception
    {
        super.init(session, options);
        getParameter(PARAM_PORT, true);
        // Override the password if it was passed as a system property
        String pwd = System.getProperty("org.openas2.ssl.Password");
        if (pwd != null)
        {
            setParameter(PARAM_SSL_KEYSTORE_PASSWORD, pwd);
        }

    }

    @Override
    public boolean healthcheck(List<String> failures)
    {
        try
        {
            String hcHost = getParameter(PARAM_ADDRESS, Properties.getProperty("ssl_host_name", "localhost"));
            String hcPort = getParameter(PARAM_PORT, true);
            String hcProtocol = getParameter(PARAM_PROTOCOL, "http");
            String urlString = hcProtocol + "://" + hcHost + ":" + hcPort + "/" + Properties.getProperty("health_check_uri", "healthcheck");

            if (logger.isTraceEnabled())
                logger.trace("Helthcheck about to try URL: " + urlString);
            Map<String, String> responseWrapper = null;
            if ("https".equalsIgnoreCase(hcProtocol))
            {
                responseWrapper = HTTPUtil.querySite(urlString, "GET", null, null);
                //responseWrapper =HTTPUtil.querySiteSSLVerifierOverride(urlString, "GET", null, null);
            }
            else responseWrapper = HTTPUtil.querySite(urlString, "GET", null, null);
            if (!"200".equals(responseWrapper.get("response_code")))
            {
                failures.add(this.getClass().getSimpleName() + " - Error making HTTP connection. Rsponse code: " + responseWrapper.get("response_code"));
                return false;
            }
        } catch (Exception e)
        {
            logger.error("Failed to execute healthcheck.", e);
            failures.add(this.getClass().getSimpleName() + " - Failed to execute HTTP connection to listener: " + e.getMessage());
            return false;
        }
        return true;
    }

    protected abstract NetModuleHandler getHandler();

    protected void handleError(Message msg, OpenAS2Exception oae)
    {
        oae.addSource(OpenAS2Exception.SOURCE_MESSAGE, msg);
        oae.terminate();

        try
        {
            CompositeParameters params = new CompositeParameters(false).
                    add("date", new DateParameters()).
                    add("msg", new MessageParameters(msg));

            String name = params.format(getParameter(PARAM_ERRORS, DEFAULT_ERRORS));
            String directory = getParameter(PARAM_ERROR_DIRECTORY, true);

            File msgFile = IOUtilOld.getUnique(IOUtilOld.getDirectoryFile(directory),
                    IOUtilOld.cleanFilename(name));
            String msgText = msg.toString();
            FileOutputStream fOut = new FileOutputStream(msgFile);

            fOut.write(msgText.getBytes());
            fOut.close();

            // make sure an error of this event is logged
            //InvalidMessageException im = new InvalidMessageException("Stored invalid message to " +
            //        msgFile.getAbsolutePath());
            //im.terminate();
        } catch (OpenAS2Exception oae2)
        {
            oae2.addSource(OpenAS2Exception.SOURCE_MESSAGE, msg);
            oae2.terminate();
        } catch (IOException ioe)
        {
            WrappedException we = new WrappedException(ioe);
            we.addSource(OpenAS2Exception.SOURCE_MESSAGE, msg);
            we.terminate();
        }
    }

    protected class ConnectionThread extends Thread {
        private NetModule owner;
        private Socket socket;

        public ConnectionThread(NetModule owner, Socket socket)
        {
            super(ClassUtils.getSimpleName(ConnectionThread.class) + "-Thread");
            this.owner = owner;
            this.socket = socket;
            start();
        }

        public NetModule getOwner()
        {
            return owner;
        }

        public Socket getSocket()
        {
            return socket;
        }

        public void run()
        {
            Socket s = getSocket();

            getOwner().getHandler().handle(getOwner(), s);

            try
            {
                s.close();
            } catch (IOException sce)
            {
                new WrappedException(sce).terminate();
            }
        }
    }

    protected class HTTPServerThread extends Thread {
        private NetModule owner;
        private ServerSocket socket;
        private boolean terminated;

        HTTPServerThread(NetModule owner, @Nullable String address, int port)
                throws IOException
        {
            super(ClassUtils.getSimpleName(HTTPServerThread.class) + " (" + defaultIfBlank(address, "0.0.0.0") + ":" + port + ")");
            this.owner = owner;
            String protocol = "http";
            String sslProtocol = "TLS";
            try
            {
                protocol = owner.getParameter(PARAM_PROTOCOL, "http");
                sslProtocol = owner.getParameter(PARAM_SSL_PROTOCOL, "TLS");
            } catch (InvalidParameterException e)
            {
                // Do nothing
            }
            if ("https".equalsIgnoreCase(protocol))
            {
                String ksName;
                char[] ksPass;
                try
                {
                    ksName = owner.getParameter(PARAM_SSL_KEYSTORE, true);
                    ksPass = owner.getParameter(PARAM_SSL_KEYSTORE_PASSWORD, true).toCharArray();
                } catch (InvalidParameterException e)
                {
                    logger.error("Required SSL parameter missing.", e);
                    throw new IOException("Failed to retireve require SSL parameters. Check config XML");
                }
                KeyStore ks;
                try
                {
                    ks = KeyStore.getInstance("JKS");
                } catch (KeyStoreException e)
                {
                    logger.error("Failed to initialise SSL keystore.", e);
                    throw new IOException("Error initialising SSL keystore");
                }

                try
                {
                    ks.load(new FileInputStream(ksName), ksPass);
                } catch (NoSuchAlgorithmException e)
                {
                    logger.error("Failed to load keystore: " + ksName, e);
                    throw new IOException("Error loading SSL keystore");
                } catch (CertificateException e)
                {
                    logger.error("Failed to load SSL certificate: " + ksName, e);
                    throw new IOException("Error loading SSL certificate");
                }
                KeyManagerFactory kmf;
                try
                {
                    kmf = KeyManagerFactory.getInstance("SunX509");
                } catch (NoSuchAlgorithmException e)
                {
                    logger.error("Failed to create key manager instance", e);
                    throw new IOException("Error creating SSL key manager instance");
                }
                try
                {
                    kmf.init(ks, ksPass);
                } catch (Exception e)
                {
                    logger.error("Failed to initialise key manager instance", e);
                    throw new IOException("Error initialising SSL key manager instance");
                }
                // setup the trust manager factory
               /* TrustManagerFactory tmf;
                try
                {
                    tmf = TrustManagerFactory.getInstance("SunX509");
                    tmf.init(ks);
                } catch (Exception e1)
                {
                    logger.error("Failed to create trust manager instance", e1);
                    throw new IOException("Error creating SSL trust manager instance");
                }*/
				
				 // Create a trust manager that does not validate certificate chains

                TrustManager[] trustAllCerts=this.getLocalTrustManager();

                if(trustAllCerts==null) {
                    logger.info("unable to get local trust manager setting default trust manager");
                    trustAllCerts = new TrustManager[]{
                            new X509TrustManager() {
                                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                                    return new java.security.cert.X509Certificate[0];
                                }

                                public void checkClientTrusted(
                                        java.security.cert.X509Certificate[] certs, String authType) {
                                }

                                public void checkServerTrusted(
                                        java.security.cert.X509Certificate[] certs, String authType) {
                                }
                            }
                    };

                }
				
                SSLContext sc;
                try
                {
                    sc = SSLContext.getInstance(sslProtocol);
                } catch (NoSuchAlgorithmException e)
                {
                    logger.error("Failed to create SSL context instance", e);
                    throw new IOException("Error creating SSL context instance");
                }
                try
                {
                    //sc.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
					sc.init(kmf.getKeyManagers(), trustAllCerts, null);
                } catch (KeyManagementException e)
                {
                    logger.error("Failed to initialise SSL context instance", e);
                    throw new IOException("Error initialising SSL context instance");
                }
                SSLServerSocketFactory ssf = sc.getServerSocketFactory();
                if (address != null)
                {
                    socket = ssf.createServerSocket(port, 0, InetAddress.getByName(address));
                } else
                {
                    socket = ssf.createServerSocket(port);
                }
                socket.setSoTimeout(5000);
            } else
            {
                socket = new ServerSocket();
                if (address != null)
                {
                    socket.bind(new InetSocketAddress(address, port));
                } else
                {
                    socket.bind(new InetSocketAddress(port));
                }
            }
        }

        public  TrustManager[] getLocalTrustManager()  {

            try {
                String strfilename = this.owner.getSession().getCertificateFactory().getParameters().get("filename");
                String strPassword = this.owner.getSession().getCertificateFactory().getParameters().get("password");
                TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("X509");

                logger.info("loading ssl store located at"+strfilename);

               // KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
                KeyStore keystore = KeyStore.getInstance("PKCS12");
                FileInputStream fin = new FileInputStream(strfilename);

                //InputStream keystoreStream = HTTPServerThread.class.getResourceAsStream(strfilename);
               // keystore.load(keystoreStream, strPassword.toCharArray());
                keystore.load(fin, strPassword.toCharArray());

                fin.close();
               /* Enumeration<String> aliasesList=keystore.aliases();
                logger.info("get alias from keystore load "+aliasesList.toString());
                /*while(aliasesList.hasMoreElements())
                {
                    logger.info("In While loop");
                    String enumAlias=aliasesList.nextElement();
                    logger.info("In While loop"+enumAlias);
                    if(!Constants.ACTIVEPARTNERCERTALIAS.contains(enumAlias))
                    {
                        try {
                            logger.info("In DELETING ALIASES"+enumAlias);
                            keystore.deleteEntry(enumAlias);
                            logger.info("remove"+enumAlias);

                            }
                            catch (Exception exp)
                            {
                                logger.info("unable to remove"+enumAlias);
                            }


                    }
                }
                logger.info("update loaded keystore");*/
                trustManagerFactory.init(keystore);
                TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
                logger.info("successfully loaded the certificate information for trust manager");
                return trustManagers;
            }
            catch (Exception exp)
            {
                logger.info("unable to load ssl store  Error is"+exp.getMessage());
                logger.info(exp);
                return null;
            }
        }

        NetModule getOwner()
        {
            return owner;
        }

        public ServerSocket getSocket()
        {
            return socket;
        }

        public boolean isTerminated()
        {
            return terminated;
        }

        public void setTerminated(boolean terminated)
        {
            this.terminated = terminated;

            if (socket != null)
            {
                try
                {
                    socket.close();
                } catch (IOException e)
                {
                    owner.forceStop(e);
                }
            }
        }

        public void run()
        {
            while (!isTerminated())
            {
                try
                {
                    Socket conn = socket.accept();
                    conn.setSoLinger(true, 60);
                    new ConnectionThread(getOwner(), conn);
                } catch (IOException e)
                {
                    if (!isTerminated())
                    {
                        owner.forceStop(e);
                    }
                }
            }
        }


        public void terminate()
        {
            setTerminated(true);
        }
    }
}
