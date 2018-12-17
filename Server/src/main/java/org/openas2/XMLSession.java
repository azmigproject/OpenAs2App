package org.openas2;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import com.fasterxml.jackson.databind.util.ISO8601DateFormat;
import javafx.util.converter.IntegerStringConverter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.openas2.app.OpenAS2Server;
import org.openas2.cert.CertificateFactory;
import org.openas2.cmd.CommandManager;
import org.openas2.cmd.CommandRegistry;
import org.openas2.cmd.processor.BaseCommandProcessor;
import org.openas2.lib.dbUtils.Properties;
import org.openas2.logging.DbLogger;
import org.openas2.logging.FileLogger;
import org.openas2.logging.LogManager;
import org.openas2.logging.Logger;
import org.openas2.partner.PartnershipFactory;
import org.openas2.partner.XMLPartnershipFactory;
import org.openas2.processor.Processor;
import org.openas2.processor.ProcessorModule;
import org.openas2.schedule.SchedulerComponent;
import org.openas2.util.XMLUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.openas2.util.AzureUtil;
import  org.openas2.lib.dbUtils.*;

/**
 * original author unknown
 * <p>
 * in this release added command registry methods
 *
 * @author joseph mcverry
 */
public class XMLSession extends BaseSession {
    private static final String EL_PROPERTIES = "properties";
    private static final String EL_CERTIFICATES = "certificates";
    private static final String EL_CMDPROCESSOR = "commandProcessors";
    private static final String EL_PROCESSOR = "processor";
    private static final String EL_PARTNERSHIPS = "partnerships";
    private static final String EL_COMMANDS = "commands";
    private static final String EL_LOGGERS = "loggers";
    private static final String PARAM_BASE_DIRECTORY = "basedir";
	private static final String EL_AZURE = "azdetails";
    private CommandRegistry commandRegistry;
    private CommandManager cmdManager = new CommandManager();
    private String VERSION;
    private String TITLE;
    private  AzureUtil azureUtil;
    private  String APIURL;
    private static final Log LOGGER = LogFactory.getLog(XMLSession.class.getSimpleName());
    private org.openas2.lib.dbUtils.Properties prop;


    public XMLSession(String apiURL,int MaxQueueDownloaderThread,int MaxFileProcessorThread,int MaxDirWatcherThread,int BlockingQueueSize
            ,int FileWatcherStalenessThresholdInSeconds, int maxRetryAttempts, int retryIntervalInSeconds ) throws OpenAS2Exception, IOException,Exception
    {
       try {


           Constants.APIURL = apiURL;
           setMaxQueueDownloaderThread(MaxQueueDownloaderThread);
           setMaxFileProcessorThread(MaxFileProcessorThread);
           setMaxDirWatcherThread(MaxDirWatcherThread);
           setBlockingQueueSizeSize(BlockingQueueSize);
           setFileWatcherStalenessThresholdInSeconds(FileWatcherStalenessThresholdInSeconds);
           setRetryAttempts(maxRetryAttempts);
           setRetryIntervalInSeconds(retryIntervalInSeconds);
           azureUtil = new AzureUtil();
           azureUtil.init();
           load(azureUtil);

           // scheduler should be initializer after all modules
           addSchedulerComponent();

                 }
       catch (Exception e)
       {
           System.out.println(e.getMessage());
           LOGGER.error(e);
           throw  e;
       }
       finally {
           if(azureUtil!=null)
           {
               azureUtil.freeResources();
               azureUtil=null;

           }
       }
    }

    public String getAPIURL()
    {
        return this.APIURL;
    }


    public Runnable reloadConfig()
    {
        return new Runnable() {
            @Override
            public void run() {
                try {

                    azureUtil = new AzureUtil();
                    azureUtil.init(true);
                    LOGGER.info("Inside reload config at "+ DateTime.now().toString() );
                    String tempLastUpdatedDateTime=azureUtil.getLastUpdatedTimeStamp();
                    if(!DateTime.parse(tempLastUpdatedDateTime).isEqual(DateTime.parse(Constants.LastUpdateTimeStamp).toInstant()))
                    {
                        LOGGER.info("in loadReqData function" );
                        loadReqData(azureUtil);
                        LOGGER.info("out loadReqData function" );
                        Constants.LastUpdateTimeStamp=tempLastUpdatedDateTime;
                    }
                    else
                    {
                        //LOGGER.info("No Diff in dates Temp1="+tempLastUpdatedDateTime+"Lastmodified="+Constants.LastUpdateTimeStamp );
                    }
                    //LOGGER.error("Method has been scheduled and running ok");
                } catch (Exception exp) {
                    System.out.println(exp.getMessage());
                    LOGGER.error(exp);
                }
                finally {

                    if(azureUtil!=null)
                    {
                        azureUtil.freeResources();
                        azureUtil=null;

                    }
                }
            };
        };

    }
   /* public XMLSession(String configAbsPath) throws OpenAS2Exception,
            ParserConfigurationException, SAXException, IOException
    {
        File configXml = new File(configAbsPath);
        File configDir = configXml.getParentFile();

        FileInputStream configAsStream = new FileInputStream(configXml);
        setBaseDirectory(configDir.getAbsolutePath());

        load(configAsStream);

        // scheduler should be initializer after all modules
        addSchedulerComponent();
    }*/

    private void addSchedulerComponent() throws OpenAS2Exception
    {
        SchedulerComponent comp = new SchedulerComponent();
        setComponent("scheduler", comp);
        comp.init(this, Collections.<String, String>emptyMap());
        comp.setReloadSession(this.reloadConfig());

    }

   protected  void load (AzureUtil azureUtil)  throws   OpenAS2Exception,Exception
   {
       loadProperties(azureUtil.getProperties());
       loadCertificates(azureUtil.getCertificates());
       loadProcessor(azureUtil.getProcessor());
       loadCommandProcessors(azureUtil.getCommandProcessors());
       loadPartnerships(azureUtil.getPartnerList(),azureUtil.getProfile(),azureUtil.getServersSettings().get(0));
       loadCommands(azureUtil.getCommand());
       loadLoggers(azureUtil);
   }


    public   void loadReqData (AzureUtil azureUtil)  throws   OpenAS2Exception,Exception
    {
        LOGGER.info("start loadReqData function" );
        this.stop();
        LOGGER.info("start loading certificates function" );
        loadCertificates(azureUtil.getCertificates());
        LOGGER.info("end loading certificates function" );
        LOGGER.info("start loading Processor function" );
        loadProcessor(azureUtil.getProcessor());
        LOGGER.info("end loading Processor function" );
        LOGGER.info("start loading Partnership function" );
        loadPartnerships(azureUtil.getPartnerList(),azureUtil.getProfile(),azureUtil.getServersSettings().get(0));
        LOGGER.info("end loading Partnership function" );
        this.start();
        LOGGER.info("stop loadReqData function" );

    }

    protected void load(InputStream in) throws ParserConfigurationException,
            SAXException, IOException, OpenAS2Exception
    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        DocumentBuilder parser = factory.newDocumentBuilder();
        Document document = parser.parse(in);
        Element root = document.getDocumentElement();

        NodeList rootNodes = root.getChildNodes();
        Node rootNode;
        String nodeName;

        // this is used by all other objects to access global configs and functionality
        LOGGER.info("Loading configuration...");
        for (int i = 0; i < rootNodes.getLength(); i++)
        {
            rootNode = rootNodes.item(i);

            nodeName = rootNode.getNodeName();

            // enter the command processing loop
            if (nodeName.equals(EL_PROPERTIES))
            {
                loadProperties(rootNode);
            } else if (nodeName.equals(EL_CERTIFICATES))
            {
                loadCertificates(rootNode);
            } else if (nodeName.equals(EL_PROCESSOR))
            {
                loadProcessor(rootNode);
            } else if (nodeName.equals(EL_CMDPROCESSOR))
            {
                loadCommandProcessors(rootNode);
            } else if (nodeName.equals(EL_PARTNERSHIPS))
            {
                loadPartnerships(rootNode);
            } else if (nodeName.equals(EL_COMMANDS))
            {
                loadCommands(rootNode);
            } else if (nodeName.equals(EL_LOGGERS))
            {
                loadLoggers(rootNode);
            }
           
			else if (nodeName.equals("#text"))
            {
                // do nothing
            } else if (nodeName.equals("#comment"))
            {
                // do nothing
            } else
            {
                throw new OpenAS2Exception("Undefined tag: " + nodeName);
            }
        }

        cmdManager.registerCommands(commandRegistry);
    }

    private void loadCommands(Commands commands ) throws OpenAS2Exception
    {
        LOGGER.info("Loading Commands...");
        Map<String, String> parameters =  new HashMap<String, String>();
        parameters.put("classname", commands.getClassName());
        parameters.put("filename", commands.getFileName());
        Map<String, Object> commandMap=new HashMap<String, Object>();
        commandMap.put("command",commands.getMulticommands());
        Component component = XMLUtil.getCommandComponent(commands.getClassName(),parameters,commandMap, this);
        commandRegistry = (CommandRegistry) component;
    }
    private void loadPartnerships(List<partner> partnerList,Profile companyProfile,ServersSettings serverSetting) throws OpenAS2Exception
    {
        LOGGER.info("Loading partnerships...");
        Map<String, String> parameters =  new HashMap<String, String>();
       // parameters.put("classname", partnerList.getClassName());
       // parameters.put("filename", partnerList.getFileName());

         //TODO ADD UPDATES TO PASS PARTNER DATA FROM partnerList

        XMLPartnershipFactory partnerFx = (XMLPartnershipFactory) XMLUtil
                .getPartnerShipComponent("org.openas2.partner.XMLPartnershipFactory",parameters, partnerList,companyProfile,serverSetting,this);

        setComponent(PartnershipFactory.COMPID_PARTNERSHIP_FACTORY, partnerFx);
    }

    private void loadProperties(org.openas2.lib.dbUtils.Properties azProperties)
    {
        LOGGER.info("Loading properties...");

        Map<String, String> properties =  new HashMap<String, String>();
        // Make key things accessible via static object for things that do not have accesss to ses sion object
        setBaseDirectory(azProperties.BasePath());

        properties.put("as2_message_id_format", azProperties.As2MessageIdFormat());
        properties.put("log_date_format", azProperties.As2MessageIdFormat());
        properties.put("sql_timestamp_format", azProperties.SqlTimestampFormat());

        properties.put(org.openas2.util.Properties. APP_TITLE_PROP, getAppTitle());
        properties.put(org.openas2.util.Properties.APP_VERSION_PROP, getAppVersion());
        org.openas2.util.Properties.setProperties(properties);
    }

    private void loadCertificates(org.openas2.lib.dbUtils.Certificates certificates) throws OpenAS2Exception
    {
        LOGGER.info("Loading certificates...");
        Map<String, String> parameters =  new HashMap<String, String>();
        parameters.put("classname", certificates.getClassName());
        parameters.put("filename", certificates.getFileName());
        parameters.put("password", certificates.getPassword());
        parameters.put("interval", String.valueOf(certificates.getInterval()));

        CertificateFactory certFx = (CertificateFactory) XMLUtil.getComponent( certificates.getClassName(),parameters, this);
        setComponent(CertificateFactory.COMPID_CERTIFICATE_FACTORY, certFx);
    }


    private void loadProcessor(org.openas2.lib.dbUtils.Processor processor) throws OpenAS2Exception
    {
        Map<String, String> parameters =  new HashMap<String, String>();
        parameters.put("classname", processor.getClassName());
        parameters.put("pendingmdn", processor.getPendingMDN());
        parameters.put("pendingmdninfo", processor.getPendingMDNInfo());
        org.openas2.processor.Processor proc = (org.openas2.processor.Processor)  XMLUtil.getComponent( processor.getClassName(),parameters, this);
        //XMLUtil.getComponent(rootNode, this);
        setComponent(Processor.COMPID_PROCESSOR, proc);
        LOGGER.info("Loading processor modules...");

        for (int i = 0; i < processor.getModules().length; i++)
        {
            org.openas2.lib.dbUtils.module mod = processor.getModules()[i];

                loadProcessorModule(proc, mod,parameters);
        }
    }

    private void loadProcessorModule(Processor proc,  org.openas2.lib.dbUtils.module mod,Map<String, String> parameters)
            throws OpenAS2Exception
    {


        parameters.put("classname", mod.getClassName());
        parameters.put("delimiters", mod.getDelimiters());
        parameters.put("errordir", mod.getErrorDir());
        parameters.put("errorformat", mod.getErrorFormat());
        parameters.put("filename", mod.getFileName());
        parameters.put("format", mod.getFormat());
        parameters.put("header", mod.getHeader());
        parameters.put("mimetype", mod.getMimetype());
        parameters.put("outboxdir", mod.getOutboxDir());
        parameters.put("protocol", mod.getProtocol());
        parameters.put("resenddir", mod.getResendDir());
        parameters.put("sendfilename", mod.getSendFileName());
        parameters.put("tempdir", mod.getTempDir());
        parameters.put("interval",  String.valueOf(mod.getInterval()));
        parameters.put("port", String.valueOf(mod.getPort()));
        parameters.put("retries", String.valueOf(mod.getRetries()));
        parameters.put("resenddelay", String.valueOf(mod.getResendDelay()));
        parameters.put("defaults", String.valueOf(mod.getDefaults()));
        parameters.put("queuename", String.valueOf(mod.getQueueName()));
        parameters.put("connectiontimeout", String.valueOf(mod.getConnectionTimeout()));
        parameters.put("readtimeout", String.valueOf(mod.getReadTimeout()));

        ProcessorModule procmod = (ProcessorModule) XMLUtil.getComponent(
                mod.getClassName(),parameters, this);
        proc.getModules().add(procmod);
    }


    private void loadCommandProcessors(List<CommandProcessors> commandProcessors) throws OpenAS2Exception
    {

        // get a registry of Command objects, and add Commands for the Session
        LOGGER.info("Loading command processor(s)...");

        for (CommandProcessors processor:commandProcessors
             ) {
                loadCommandProcessor(cmdManager, processor);
        }
    }

    private void loadCommandProcessor(CommandManager manager,
                                      CommandProcessors processor) throws OpenAS2Exception
    {

        Map<String, String> parameters =  new HashMap<String, String>();
        parameters.put("classname", processor.getClassName());
        parameters.put("userId", processor.getUserName());
        parameters.put("password", processor.getPassword());
        parameters.put("portId", String.valueOf(processor.getPort()));

        BaseCommandProcessor cmdProcesor = (BaseCommandProcessor) XMLUtil
                .getComponent( processor.getClassName(),parameters, this);
        manager.addProcessor(cmdProcesor);

        setComponent(cmdProcesor.getName(), cmdProcesor);
    }


    private void loadProperties(Node propNode)
    {
        LOGGER.info("Loading properties...");

        Map<String, String> properties = XMLUtil.mapAttributes(propNode, false);
        // Make key things accessible via static object for things that do not have accesss to session object
        properties.put(org.openas2.util.Properties.APP_TITLE_PROP, getAppTitle());
        properties.put(org.openas2.util.Properties.APP_VERSION_PROP, getAppVersion());
        org.openas2.util.Properties.setProperties(properties);
    }

    private void loadCertificates(Node rootNode) throws OpenAS2Exception
    {
        CertificateFactory certFx = (CertificateFactory) XMLUtil.getComponent(
                rootNode, this);
        setComponent(CertificateFactory.COMPID_CERTIFICATE_FACTORY, certFx);
    }

    private void loadCommands(Node rootNode) throws OpenAS2Exception
    {
        Component component = XMLUtil.getComponent(rootNode, this);
        commandRegistry = (CommandRegistry) component;
    }
    private  void loadLoggers(AzureUtil azureUtil) throws OpenAS2Exception,Exception
    {
        LOGGER.info("Loading log manager(s)...");

        LogManager manager = LogManager.getLogManager();
        LOGGER.info( "LOG_TABLE_NAME=="+azureUtil.LOG_TABLE_NAME);
        LOGGER.info( "LOG_COUNT_TABLE_NAME=="+azureUtil.LOG_COUNT_TABLE_NAME);
        LOGGER.info( "STORAGE_CONNECTION_STRING=="+azureUtil.STORAGE_CONNECTION_STRING);
        Logger logger =  new DbLogger(azureUtil.LOG_TABLE_NAME, azureUtil.LOG_COUNT_TABLE_NAME, azureUtil.STORAGE_CONNECTION_STRING);
        Logger filelogger =  new FileLogger();
         manager.addLogger(filelogger);
        manager.addLogger(logger);
    }
    private void loadLoggers(Node rootNode) throws OpenAS2Exception
    {
        LOGGER.info("Loading log manager(s)...");

        LogManager manager = LogManager.getLogManager();
       /* if (LogManager.isRegisteredWithApache())
        {
            ; // continue
        } else
        {
            // if using the OpenAS2 loggers the log manager must registered with the jvm argument
            // -Dorg.apache.commons.logging.Log=org.openas2.logging.Log
            throw new OpenAS2Exception("the OpenAS2 loggers' log manager must registered with the jvm argument -Dorg.apache.commons.logging.Log=org.openas2.logging.Log");
        }*/
        NodeList loggers = rootNode.getChildNodes();
        Node logger;

        for (int i = 0; i < loggers.getLength(); i++)
        {
            logger = loggers.item(i);

            if (logger.getNodeName().equals("logger"))
            {
                loadLogger(manager, logger);
            }
        }
    }

    private void loadLogger(LogManager manager, Node loggerNode)
            throws OpenAS2Exception
    {
        Logger logger = (Logger) XMLUtil.getComponent(loggerNode, this);
        manager.addLogger(logger);
    }

    private void loadCommandProcessors(Node rootNode) throws OpenAS2Exception
    {

        // get a registry of Command objects, and add Commands for the Session
        LOGGER.info("Loading command processor(s)...");

        NodeList cmdProcessor = rootNode.getChildNodes();
        Node processor;

        for (int i = 0; i < cmdProcessor.getLength(); i++)
        {
            processor = cmdProcessor.item(i);

            if (processor.getNodeName().equals("commandProcessor"))
            {
                loadCommandProcessor(cmdManager, processor);
            }
        }
    }

    private void loadCommandProcessor(CommandManager manager,
                                      Node cmdPrcessorNode) throws OpenAS2Exception
    {
        BaseCommandProcessor cmdProcesor = (BaseCommandProcessor) XMLUtil
                .getComponent(cmdPrcessorNode, this);
        manager.addProcessor(cmdProcesor);

        setComponent(cmdProcesor.getName(), cmdProcesor);
    }

    private void loadPartnerships(Node rootNode) throws OpenAS2Exception
    {
        LOGGER.info("Loading partnerships...");

        PartnershipFactory partnerFx = (PartnershipFactory) XMLUtil
                .getComponent(rootNode, this);
        setComponent(PartnershipFactory.COMPID_PARTNERSHIP_FACTORY, partnerFx);
    }

    private void loadProcessor(Node rootNode) throws OpenAS2Exception
    {
        Processor proc = (Processor) XMLUtil.getComponent(rootNode, this);
        setComponent(Processor.COMPID_PROCESSOR, proc);

        LOGGER.info("Loading processor modules...");

        NodeList modules = rootNode.getChildNodes();
        Node module;

        for (int i = 0; i < modules.getLength(); i++)
        {
            module = modules.item(i);

            if (module.getNodeName().equals("module"))
            {
                loadProcessorModule(proc, module);
            }
        }
    }

    private void loadProcessorModule(Processor proc, Node moduleNode)
            throws OpenAS2Exception
    {
        ProcessorModule procmod = (ProcessorModule) XMLUtil.getComponent(
                moduleNode, this);
        proc.getModules().add(procmod);
    }

    @Nullable
    private String getManifestAttribValue(@Nonnull String attrib)
    {
        Enumeration<?> resEnum;
        try
        {
            resEnum = Thread.currentThread().getContextClassLoader().getResources(JarFile.MANIFEST_NAME);
            while (resEnum.hasMoreElements())
            {
                try
                {
                    URL url = (URL) resEnum.nextElement();
                    if (!url.getPath().contains("openas2"))
                    {
                        continue;
                    }
                    InputStream is = url.openStream();
                    if (is != null)
                    {
                        Manifest manifest = new Manifest(is);
                        Attributes mainAttribs = manifest.getMainAttributes();
                        String value = mainAttribs.getValue(attrib);
                        if (value != null)
                        {
                            return value;
                        }
                    }
                } catch (Exception e)
                {
                    // Silently ignore wrong manifests on classpath?
                }
            }
        } catch (IOException e1)
        {
            // Silently ignore wrong manifests on classpath?
        }
        return null;
    }

    public String getAppVersion()
    {
        if (VERSION == null)
        {
            VERSION = getManifestAttribValue("Implementation-Version");
        }
        return VERSION;
    }

    public String getAppTitle()
    {
        if (TITLE == null)
        {
            TITLE = getManifestAttribValue("Implementation-Title") + " v" + getAppVersion();
        }
        return TITLE;

    }
}
