package org.openas2.partner;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openas2.OpenAS2Exception;
import org.openas2.lib.dbUtils.Profile;
import org.openas2.lib.dbUtils.ServersSettings;
import org.openas2.lib.dbUtils.partner;
import org.openas2.schedule.HasSchedule;
import org.openas2.Session;
import org.openas2.WrappedException;
import org.openas2.params.InvalidParameterException;
import org.openas2.support.FileMonitorAdapter;
import org.openas2.util.XMLUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * original author unknown
 * <p>
 * this release added logic to store partnerships and provide methods for partner/partnership command line processor
 *
 * @author joseph mcverry
 */
public class XMLPartnershipFactory extends BasePartnershipFactory{
//public class XMLPartnershipFactory extends BasePartnershipFactory implements HasSchedule {
    public static final String PARAM_FILENAME = "filename";
    public static final String PARAM_INTERVAL = "interval";

    private Map<String, Object> partners;

    private List<partner>  _partnerFromDB;
    private List<Profile>  _profileFromDB;

    public List<partner>  getPartnersFromDB(){return _partnerFromDB;}
    public void setPartnersFromDB(List<partner>  partners){this._partnerFromDB=partners;}

    public List<Profile>  getProfileFromDB(){return _profileFromDB;}
    public void setProfileFromDB(List<Profile>  profiles){this._profileFromDB=profiles;}

    private Profile _companyProfile;

    private ServersSettings _serverSettings;

    public Profile  getCompanyProfile(){return _companyProfile;}
    public void setCompanyProfile(Profile  profile){this._companyProfile=profile;}

    public ServersSettings  getServerSettings(){return _serverSettings;}
    public void setServerSettings(ServersSettings  serverSettings){this._serverSettings=serverSettings;}


    private Log logger = LogFactory.getLog(XMLPartnershipFactory.class.getSimpleName());


    private int getRefreshInterval() throws InvalidParameterException
    {
        return getParameterInt(PARAM_INTERVAL, false);
    }

    String getFilename() throws InvalidParameterException
    {
        return getParameter(PARAM_FILENAME, true);
    }

    public Map<String, Object> getPartners()
    {
        if (partners == null)
        {
            partners = new HashMap<String, Object>();
        }

        return partners;
    }

    private void setPartners(Map<String, Object> map)
    {

        partners = map;

    }

    public void init(Session session, Map<String, String> parameters) throws OpenAS2Exception
    {
        super.init(session, parameters);
        if(_partnerFromDB!=null && _profileFromDB!=null)
        {

            refresh(_partnerFromDB,_profileFromDB);
        }
        if(_partnerFromDB!=null)
        {

             refresh(_partnerFromDB);
        }
        else {
            refresh();
        }
    }

    void refresh(List<partner>  partners,List<Profile>  profiles) throws OpenAS2Exception
    {

        try {
            Map<String, Object> newPartners = new HashMap<String, Object>();
            List<Partnership> newPartnerships = new ArrayList<Partnership>();
            partner companyPartner=new partner();
            companyPartner.setEmailAddress(_companyProfile.getEmailAddress());
            companyPartner.setPartnerName(Profile.PROFILENAME);
            companyPartner.setAS2Identifier(_companyProfile.getAS2Idenitfier());
            //companyPartner.setPublicCertificate(_companyProfile.getPublicCertificate());
            companyPartner.setPublicCertificate(_companyProfile.getPrivateCertificate());

            loadPartner(newPartners, companyPartner);

            Map<String ,String> PartnerToServer=new HashMap<String, String>();
            PartnerToServer.put("receiver",Profile.PROFILENAME);
            PartnerToServer.put("protocol","AS2");

            PartnerToServer.put("blobContainer",_serverSettings.getBlobContainerName());
            PartnerToServer.put("MaxFileSize_Queue",String.valueOf( _serverSettings.getMaxFileSize()));
            PartnerToServer.put("content_transfer_encoding","8bit");
            PartnerToServer.put("mdnsubject","Your requested MDN response from $receiver.as2_id$");
            PartnerToServer.put("as2_mdn_to",companyPartner.getEmailAddress());
            PartnerToServer.put("prevent_canonicalization_for_mic","false");
            PartnerToServer.put("no_set_transfer_encoding_for_signing","false");
            PartnerToServer.put("rename_digest_to_old_name","false");
            PartnerToServer.put("emove_cms_algorithm_protection_attrib","false");


            for (partner Partner : partners
                    ) {


                loadPartner(newPartners, Partner);


            }
            for (partner Partner : partners
                    ) {
                PartnerToServer.put("Inqueue",Partner.getIncomingQueue());
                PartnerToServer.put("Outqueue",Partner.getOutgoingQueue());
                PartnerToServer.put("SentQueue",Partner.getSentQueue());
                PartnerToServer.put("InqueueError",Partner.getInErrorQueue());
                //PartnerToServer.put("OutqueueError",Partner.getOutErrorQueue());
                PartnerToServer.put("sender",Partner.getPartnerName());
                PartnerToServer.put("name",Partner.getPartnerName()+"-to-"+Profile.PROFILENAME);
                PartnerToServer.put("subject","AS2 Message From "+ Partner.getPartnerName() +" to serverProfile");
               if(Partner.getIncomingMessageRequireEncryption())
                PartnerToServer.put("encrypt",Partner.getEncryptionAlgorithm());
                if(Partner.getIncomingMessageRequireSignature()) PartnerToServer.put("sign",Partner.getSignatureAlgorithm());
                PartnerToServer.put("resend_max_retries",String.valueOf(Partner.getMaxAttempts()));
                if(Partner.getISMDNSigned())
                {
                    PartnerToServer.put("as2_mdn_options","signed-receipt-protocol=optional, pkcs7-signature; signed-receipt-micalg=optional, SHA1");
                }
                if(!Partner.getIsSyncronous())
                {
                    PartnerToServer.put("as2_url",_companyProfile.getAsynchronousMDNURL());
                }
                if(logger.isDebugEnabled())
                logger.info ("PartnerCreated"+Partner.getPartnerName()+"-to-"+Profile.PROFILENAME + "IsEncomingEncrypt"
                        +(Partner.getIncomingMessageRequireEncryption()? PartnerToServer.get("encrypt"):"false")+
                         "IsEncomingsign"+(Partner.getIncomingMessageRequireSignature()? PartnerToServer.get("sign"):"false") );
                Map<String ,String> ServerToPartner=new HashMap<String, String>();
                ServerToPartner.put("sender",Profile.PROFILENAME);
                ServerToPartner.put("blobContainer",_serverSettings.getBlobContainerName());
                ServerToPartner.put("MaxFileSize_Queue",String.valueOf( _serverSettings.getMaxFileSize()));
                ServerToPartner.put("Inqueue",Partner.getIncomingQueue());
                ServerToPartner.put("subject","AS2 Message From serverProfile to "+Partner.getPartnerName());
                ServerToPartner.put("Outqueue",Partner.getOutgoingQueue());
                ServerToPartner.put("SentQueue",Partner.getSentQueue());
                ServerToPartner.put("InqueueError",Partner.getInErrorQueue());
                //ServerToPartner.put("OutqueueError",Partner.getOutErrorQueue());
                ServerToPartner.put("name",Profile.PROFILENAME+"-to-"+Partner.getPartnerName());
                ServerToPartner.put("receiver",Partner.getPartnerName());
                ServerToPartner.put("protocol","AS2");
                ServerToPartner.put("content_transfer_encoding","8bit");
                ServerToPartner.put("mdnsubject","Your requested MDN response from $receiver.as2_id$");
                ServerToPartner.put("as2_mdn_to",Partner.getEmailAddress());
                ServerToPartner.put("prevent_canonicalization_for_mic","false");
                ServerToPartner.put("no_set_transfer_encoding_for_signing","false");
                ServerToPartner.put("rename_digest_to_old_name","false");
                ServerToPartner.put("emove_cms_algorithm_protection_attrib","false");
                if(Partner.getISMDNSigned())
                {
                    ServerToPartner.put("as2_mdn_options","signed-receipt-protocol=optional, pkcs7-signature; signed-receipt-micalg=optional, SHA1");
                }
                if(!Partner.getIsSyncronous())
                {
                    ServerToPartner.put("as2_url",Partner.getPartnerUrl());
                }
                if(Partner.getEncryptOutgoingMessage())ServerToPartner.put("encrypt",Partner.getEncryptionAlgorithm());
                if(Partner.getSignOutgoingMessage())ServerToPartner.put("sign",Partner.getSignatureAlgorithm());
                ServerToPartner.put("resend_max_retries",String.valueOf(Partner.getMaxAttempts()));
                if(logger.isDebugEnabled())
                logger.info("ServerToPartnerCreated"+ServerToPartner.get("name") + "IsoutgoingEncrypt"+
                        (Partner.getEncryptOutgoingMessage()?ServerToPartner.get("encrypt"):"false")+ "Isoutgoingsign"+(Partner.getSignOutgoingMessage()? ServerToPartner.get("sign"):"false") );

                loadPartnership(newPartners, newPartnerships, ServerToPartner);
                loadPartnership(newPartners, newPartnerships, PartnerToServer);


            }

            for (Profile tempProfile : _profileFromDB
                    ) {
                if(tempProfile.getIsMainProfile()!=true) {

                    partner profilesPartner=new partner();
                    profilesPartner.setEmailAddress(tempProfile.getEmailAddress());
                    profilesPartner.setPartnerName(tempProfile.getAS2Idenitfier());
                    profilesPartner.setAS2Identifier(tempProfile.getAS2Idenitfier());
                    //companyPartner.setPublicCertificate(_companyProfile.getPublicCertificate());
                    profilesPartner.setPublicCertificate(tempProfile.getPrivateCertificate());
                    loadPartner(newPartners, profilesPartner);
                }


            }
            synchronized (this)
            {
                setPartners(newPartners);
                setPartnerships(newPartnerships);

            }
        }
        catch (Exception e)
        {
            throw new WrappedException(e);
        }
    }

    void refresh(List<partner>  partners) throws OpenAS2Exception
    {

        try {
            Map<String, Object> newPartners = new HashMap<String, Object>();
            List<Partnership> newPartnerships = new ArrayList<Partnership>();
            partner companyPartner=new partner();
            companyPartner.setEmailAddress(_companyProfile.getEmailAddress());
            companyPartner.setPartnerName(Profile.PROFILENAME);
            companyPartner.setAS2Identifier(_companyProfile.getAS2Idenitfier());
            //companyPartner.setPublicCertificate(_companyProfile.getPublicCertificate());
            companyPartner.setPublicCertificate(_companyProfile.getPrivateCertificate());

            loadPartner(newPartners, companyPartner);

            Map<String ,String> PartnerToServer=new HashMap<String, String>();
            PartnerToServer.put("receiver",Profile.PROFILENAME);
            PartnerToServer.put("protocol","AS2");

            PartnerToServer.put("blobContainer",_serverSettings.getBlobContainerName());
            PartnerToServer.put("MaxFileSize_Queue",String.valueOf( _serverSettings.getMaxFileSize()));
            PartnerToServer.put("content_transfer_encoding","8bit");
            PartnerToServer.put("mdnsubject","Your requested MDN response from $receiver.as2_id$");
            PartnerToServer.put("as2_mdn_to",companyPartner.getEmailAddress());
            PartnerToServer.put("prevent_canonicalization_for_mic","false");
            PartnerToServer.put("no_set_transfer_encoding_for_signing","false");
            PartnerToServer.put("rename_digest_to_old_name","false");
            PartnerToServer.put("emove_cms_algorithm_protection_attrib","false");


            for (partner Partner : partners
                    ) {


                    loadPartner(newPartners, Partner);


            }
            for (partner Partner : partners
                    ) {
                PartnerToServer.put("Inqueue",Partner.getIncomingQueue());
                PartnerToServer.put("Outqueue",Partner.getOutgoingQueue());
                PartnerToServer.put("SentQueue",Partner.getSentQueue());
                PartnerToServer.put("InqueueError",Partner.getInErrorQueue());
                //PartnerToServer.put("OutqueueError",Partner.getOutErrorQueue());
                PartnerToServer.put("sender",Partner.getPartnerName());
                PartnerToServer.put("name",Partner.getPartnerName()+"-to-"+Profile.PROFILENAME);
                PartnerToServer.put("subject","AS2 Message From "+ Partner.getPartnerName() +" to serverProfile");
                PartnerToServer.put("encrypt",Partner.getEncryptionAlgorithm());
                PartnerToServer.put("sign",Partner.getSignatureAlgorithm());
                PartnerToServer.put("resend_max_retries",String.valueOf(Partner.getMaxAttempts()));
                if(Partner.getISMDNSigned())
                {
                    PartnerToServer.put("as2_mdn_options","signed-receipt-protocol=optional, pkcs7-signature; signed-receipt-micalg=optional, SHA1");
                }
                if(!Partner.getIsSyncronous())
                {
                    PartnerToServer.put("as2_url",_companyProfile.getAsynchronousMDNURL());
                }

                Map<String ,String> ServerToPartner=new HashMap<String, String>();
                ServerToPartner.put("sender",Profile.PROFILENAME);
                ServerToPartner.put("blobContainer",_serverSettings.getBlobContainerName());
                ServerToPartner.put("MaxFileSize_Queue",String.valueOf( _serverSettings.getMaxFileSize()));
                ServerToPartner.put("Inqueue",Partner.getIncomingQueue());
                ServerToPartner.put("subject","AS2 Message From serverProfile to "+Partner.getPartnerName());
                ServerToPartner.put("Outqueue",Partner.getOutgoingQueue());
                ServerToPartner.put("SentQueue",Partner.getSentQueue());
                ServerToPartner.put("InqueueError",Partner.getInErrorQueue());
                //ServerToPartner.put("OutqueueError",Partner.getOutErrorQueue());
                ServerToPartner.put("name",Profile.PROFILENAME+"-to-"+Partner.getPartnerName());
                ServerToPartner.put("receiver",Partner.getPartnerName());
                ServerToPartner.put("protocol","AS2");
                ServerToPartner.put("content_transfer_encoding","8bit");
                ServerToPartner.put("mdnsubject","Your requested MDN response from $receiver.as2_id$");
                ServerToPartner.put("as2_mdn_to",Partner.getEmailAddress());
                ServerToPartner.put("prevent_canonicalization_for_mic","false");
                ServerToPartner.put("no_set_transfer_encoding_for_signing","false");
                ServerToPartner.put("rename_digest_to_old_name","false");
                ServerToPartner.put("emove_cms_algorithm_protection_attrib","false");
                if(Partner.getISMDNSigned())
                {
                    ServerToPartner.put("as2_mdn_options","signed-receipt-protocol=optional, pkcs7-signature; signed-receipt-micalg=optional, SHA1");
                }
                if(!Partner.getIsSyncronous())
                {
                    ServerToPartner.put("as2_url",Partner.getPartnerUrl());
                }
                ServerToPartner.put("encrypt",Partner.getEncryptionAlgorithm());
                ServerToPartner.put("sign",Partner.getSignatureAlgorithm());
                ServerToPartner.put("resend_max_retries",String.valueOf(Partner.getMaxAttempts()));


                loadPartnership(newPartners, newPartnerships, ServerToPartner);
                loadPartnership(newPartners, newPartnerships, PartnerToServer);


            }
            synchronized (this)
            {
                setPartners(newPartners);
                setPartnerships(newPartnerships);
            }
        }
        catch (Exception e)
        {
            throw new WrappedException(e);
        }
    }

    public void loadPartner(Map<String, Object> partners, Node node)
            throws OpenAS2Exception
    {
        String[] requiredAttributes = {"name"};

        Map<String, String> newPartner = XMLUtil.mapAttributes(node, requiredAttributes);
        String name = newPartner.get("name");

        if (partners.get(name) != null)
        {
            throw new OpenAS2Exception("Partner is defined more than once: " + name);
        }

        partners.put(name, newPartner);
    }




    void refresh() throws OpenAS2Exception
    {
        FileInputStream inputStream = null;
        try
        {
            inputStream = new FileInputStream(getFilename());
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

            DocumentBuilder parser = factory.newDocumentBuilder();
            Document document = parser.parse(inputStream);
            Element root = document.getDocumentElement();
            NodeList rootNodes = root.getChildNodes();
            Node rootNode;
            String nodeName;

            Map<String, Object> newPartners = new HashMap<String, Object>();
            List<Partnership> newPartnerships = new ArrayList<Partnership>();

            for (int i = 0; i < rootNodes.getLength(); i++)
            {
                rootNode = rootNodes.item(i);

                nodeName = rootNode.getNodeName();

                if (nodeName.equals("partner"))
                {
                    loadPartner(newPartners, rootNode);
                } else if (nodeName.equals("partnership"))

                {
                    loadPartnership(newPartners, newPartnerships, rootNode);
                }
            }

            synchronized (this)
            {
                setPartners(newPartners);
                setPartnerships(newPartnerships);
            }
        } catch (Exception e)
        {
            throw new WrappedException(e);
        } finally
        {
            IOUtils.closeQuietly(inputStream);
        }
    }

    private void loadAttributes(Node node, Partnership partnership)
            throws OpenAS2Exception
    {
        Map<String, String> nodes = XMLUtil.mapAttributeNodes(node.getChildNodes(), "attribute", "name", "value");

        partnership.getAttributes().putAll(nodes);
    }


    private void loadAttributes( Map<String, String> attributes, Partnership partnership)

    {

        partnership.getAttributes().putAll(attributes);
    }

    public void loadPartner(Map<String, Object> partners, partner Partner)
            throws OpenAS2Exception
    {
        String[] requiredAttributes = {"name"};

        Map<String, String> newPartner =new HashMap<String, String>();
        newPartner.put("name",Partner.getPartnerName());
        newPartner.put("as2_id",Partner.getAS2Identifier());
        newPartner.put("x509_alias",Partner.getPublicCertificate());
        newPartner.put("email",Partner.getEmailAddress());
        newPartner=XMLUtil.mapAttributes(newPartner, requiredAttributes);

        String name = newPartner.get("name");

        if (partners.get(name) != null)
        {
            throw new OpenAS2Exception("Partner is defined more than once: " + name);
        }

        partners.put(name, newPartner);
    }


    public void loadPartnership(Map<String, Object> partners, List<Partnership> partnerships, Map<String ,String> newPartnership )
            throws OpenAS2Exception
    {
        Partnership partnership = new Partnership();
        String[] requiredAttributes = {"name"};

        Map<String, String> psAttributes=new HashMap<String, String>();
        psAttributes=XMLUtil.mapAttributes(newPartnership, requiredAttributes);
        String name = psAttributes.get("name");

        if (getPartnership(partnerships, name) != null)
        {
            throw new OpenAS2Exception("Partnership is defined more than once: " + name);
        }

        partnership.setName(name);

        // load the sender and receiver information
        loadPartnerIDs(partners, name, newPartnership, "sender", partnership.getSenderIDs());
        loadPartnerIDs(partners, name, newPartnership, "receiver", partnership.getReceiverIDs());

        // read in the partnership attributes
        loadAttributes(newPartnership, partnership);

        // add the partnership to the list of available partnerships
        partnerships.add(partnership);
    }

    private void loadPartnerIDs(Map<String, Object> partners, String partnershipName, Map<String ,String> partnershipNode,
                                String partnerType, Map<String, Object> idMap) throws OpenAS2Exception
    {


        boolean partnerNode = partnershipNode.containsKey(partnerType);

        if (!partnerNode)
        {
            throw new OpenAS2Exception("Partnership " + partnershipName + " is missing "+partnerType);
        }


        // check for a partner name, and look up in partners list if one is found
        String partnerName =  partnershipNode.get(partnerType);

        if (partnerName != null)
        {
            Map<String, Object> map = (Map<String, Object>) partners.get(partnerName);
            Map<String, Object> partner = map;

            if (partner == null)
            {
                throw new OpenAS2Exception("Partnership " + partnershipName + " has an undefined " +
                        partnerType + ": " + partnerName);
            }

            idMap.putAll(partner);
        }

        // copy all other attributes to the partner id map		
        idMap.putAll(partnershipNode);
    }

    private void loadPartnerIDs(Map<String, Object> partners, String partnershipName, Node partnershipNode,
                                String partnerType, Map<String, Object> idMap) throws OpenAS2Exception
    {
        Node partnerNode = XMLUtil.findChildNode(partnershipNode, partnerType);

        if (partnerNode == null)
        {
            throw new OpenAS2Exception("Partnership " + partnershipName + " is missing sender");
        }

        Map<String, String> partnerAttr = XMLUtil.mapAttributes(partnerNode);

        // check for a partner name, and look up in partners list if one is found
        String partnerName = partnerAttr.get("name");

        if (partnerName != null)
        {
            Map<String, Object> map = (Map<String, Object>) partners.get(partnerName);
            Map<String, Object> partner = map;

            if (partner == null)
            {
                throw new OpenAS2Exception("Partnership " + partnershipName + " has an undefined " +
                        partnerType + ": " + partnerName);
            }

            idMap.putAll(partner);
        }

        // copy all other attributes to the partner id map
        idMap.putAll(partnerAttr);
    }

    public void loadPartnership(Map<String, Object> partners, List<Partnership> partnerships, Node node)
            throws OpenAS2Exception
    {
        Partnership partnership = new Partnership();
        String[] requiredAttributes = {"name"};

        Map<String, String> psAttributes = XMLUtil.mapAttributes(node, requiredAttributes);
        String name = psAttributes.get("name");

        if (getPartnership(partnerships, name) != null)
        {
            throw new OpenAS2Exception("Partnership is defined more than once: " + name);
        }

        partnership.setName(name);

        // load the sender and receiver information
        loadPartnerIDs(partners, name, node, "sender", partnership.getSenderIDs());
        loadPartnerIDs(partners, name, node, "receiver", partnership.getReceiverIDs());

        // read in the partnership attributes
        loadAttributes(node, partnership);

        // add the partnership to the list of available partnerships
        partnerships.add(partnership);
    }

    public void storePartnership()
            throws OpenAS2Exception
    {
        String fn = getFilename();


        DecimalFormat df = new DecimalFormat("0000000");
        long l = 0;
        File f = null;
        while (true)
        {
            f = new File(fn + '.' + df.format(l));
            if (f.exists() == false)
            {
                break;
            }
            l++;
        }

        logger.info("backing up " + fn + " to " + f.getName());

        File fr = new File(fn);
        fr.renameTo(f);

        try
        {
            PrintWriter pw = new PrintWriter(new FileOutputStream(fn));


            Map<String, Object> partner = partners;
            pw.println("<partnerships>");
            Iterator<Map.Entry<String, Object>> partnerIt = partner.entrySet().iterator();
            while (partnerIt.hasNext())
            {
                Map.Entry<String, Object> ptrnData = partnerIt.next();
                HashMap<String, Object> partnerMap = (HashMap<String, Object>) ptrnData.getValue();
                pw.print("  <partner ");
                Iterator<Map.Entry<String, Object>> attrIt = partnerMap.entrySet().iterator();
                while (attrIt.hasNext())
                {
                    Map.Entry<String, Object> attribute = attrIt.next();
                    pw.print(attribute.getKey() + "=\"" + attribute.getValue() + "\"");
                    if (attrIt.hasNext())
                    {
                        pw.print("\n           ");
                    }
                }
                pw.println("/>");
            }
            List<Partnership> partnerShips = getPartnerships();
            ListIterator<Partnership> partnerLIt = partnerShips.listIterator();
            while (partnerLIt.hasNext())
            {
                Partnership partnership = partnerLIt.next();
                pw.println("  <partnership name=\"" + partnership.getName() + "\">");
                pw.println("    <sender name=\"" + partnership.getSenderIDs().get("name") + "\"/>");
                pw.println("    <receiver name=\"" + partnership.getReceiverIDs().get("name") + "\"/>");
                Map<String, String> partnershipMap = partnership.getAttributes();

                Iterator<Map.Entry<String, String>> partnershipIt = partnershipMap.entrySet().iterator();
                while (partnershipIt.hasNext())
                {
                    Map.Entry<String, String> partnershipData = partnershipIt.next();
                    pw.println("    <attribute name=\"" + partnershipData.getKey() + "\" value=\"" + partnershipData.getValue() + "\"/>");

                }
                pw.println("  </partnership>");
            }
            pw.println("</partnerships>");
            pw.flush();
            pw.close();
        } catch (FileNotFoundException e)
        {
            throw new WrappedException(e);
        }
    }

   /* @Override
    public void schedule(ScheduledExecutorService executor) throws OpenAS2Exception
    {
        new FileMonitorAdapter() {
            @Override
            public void onConfigFileChanged() throws OpenAS2Exception
            {
                refresh();
                logger.debug("- Partnerships Reloaded -");
            }
        }.scheduleIfNeed(executor, new File(getFilename()), getRefreshInterval(), TimeUnit.SECONDS);
    }*/
}
