package org.openas2.cert;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openas2.OpenAS2Exception;
import org.openas2.Session;
import org.openas2.WrappedException;
import org.openas2.message.Message;
import org.openas2.message.MessageMDN;
import org.openas2.params.InvalidParameterException;
import org.openas2.partner.Partnership;
import org.openas2.partner.SecurePartnership;
import org.openas2.schedule.HasSchedule;
import org.openas2.support.FileMonitorAdapter;
import org.openas2.util.AS2Util;

public class PKCS12CertificateFactory extends BaseCertificateFactory implements
        AliasedCertificateFactory, KeyStoreCertificateFactory, StorableCertificateFactory,
        HasSchedule {
    public static final String PARAM_FILENAME = "filename";
    public static final String PARAM_PASSWORD = "password";
    public static final String PARAM_INTERVAL = "interval";
    private KeyStore keyStore;

    private Log logger = LogFactory.getLog(PKCS12CertificateFactory.class.getSimpleName());

    public String getAlias(Partnership partnership, String partnershipType) throws OpenAS2Exception
    {
        String alias = null;
        logger.info("Find  Alias for Partnership type as "+partnershipType+ " and Partnership is "+partnership.toString());
        if (partnershipType == Partnership.PTYPE_RECEIVER)
        {
            alias = partnership.getReceiverID(SecurePartnership.PID_X509_ALIAS);
            logger.info("Alias foud for receiver is "+alias);
        } else if (partnershipType == Partnership.PTYPE_SENDER)
        {
            alias = partnership.getSenderID(SecurePartnership.PID_X509_ALIAS);
            logger.info("Alias foud for sender is "+alias);
        }

        if (alias == null)
        {
            throw new CertificateNotFoundException(partnershipType, null);
        }

        return alias;
    }


    public String getSigningAlias(Partnership partnership, String partnershipType) throws OpenAS2Exception
    {
        String alias = null;
        logger.info("Find signing Alias for  Partnership type as "+partnershipType+ " and Partnership is "+partnership.toString());
        if (partnershipType == Partnership.PTYPE_RECEIVER)
        {
            alias = partnership.getReceiverID(SecurePartnership.PID_X509_SignALIAS);
            logger.info("Signing Alias found for receiver is "+alias);
        } else if (partnershipType == Partnership.PTYPE_SENDER)
        {
            alias = partnership.getSenderID(SecurePartnership.PID_X509_SignALIAS);
            logger.info("Signing Alias found for sender is "+alias);
        }

        if (alias == null|| alias.trim().equalsIgnoreCase(""))
        {
            logger.info("Signing Alias for  Partnership not found. Getting default alias");

            alias=getAlias(partnership, partnershipType);

            logger.info("Default Alias found for sender is "+alias);
        }

        return alias;
    }

    public X509Certificate getCertificate(String alias) throws OpenAS2Exception
    {
        try
        {
            logger.info("Fetching certificate for alias"+ alias);
            KeyStore ks = getKeyStore();
            X509Certificate cert = (X509Certificate) ks.getCertificate(alias);
            logger.info("Cert for alias"+alias+" is " +cert.getIssuerDN()+","+cert.getSigAlgName()+","+cert.getSubjectDN());
            if (cert == null)
            {
                throw new CertificateNotFoundException(null, alias);
            }

            return cert;
        } catch (KeyStoreException kse)
        {
            throw new WrappedException(kse);
        }
    }

    public X509Certificate getCertificate(Message msg, String partnershipType)
            throws OpenAS2Exception
    {
        try
        {
            logger.info("In getCertificate with MessageID="+msg.getMessageID()+" and partnershipType="+partnershipType);
            return getCertificate(getAlias(msg.getPartnership(), partnershipType));
        } catch (CertificateNotFoundException cnfe)
        {
            cnfe.setPartnershipType(partnershipType);
            throw cnfe;
        }
    }

    public X509Certificate getSigningCertificate(Message msg, String partnershipType)
            throws OpenAS2Exception
    {
        try
        {
            logger.info("In getSigningCertificate with MessageID="+msg.getMessageID()+" and partnershipType="+partnershipType);
            return getCertificate(getSigningAlias(msg.getPartnership(), partnershipType));
        } catch (CertificateNotFoundException cnfe)
        {
            cnfe.setPartnershipType(partnershipType);
            throw cnfe;
        }
    }

    public X509Certificate getCertificate(MessageMDN mdn, String partnershipType)
            throws OpenAS2Exception
    {
        try
        {
            logger.info("In getCertificate with MessageMDN="+mdn.getMessageID()+" and partnershipType="+partnershipType);
            return getCertificate(getAlias(mdn.getPartnership(), partnershipType));
        } catch (CertificateNotFoundException cnfe)
        {
            cnfe.setPartnershipType(partnershipType);
            throw cnfe;
        }
    }

    public X509Certificate getSigningCertificate(MessageMDN mdn, String partnershipType)
            throws OpenAS2Exception
    {
        try
        {
            logger.info("In getSigningCertificate with MessageMDN="+mdn.getMessageID()+" and partnershipType="+partnershipType);
            return getCertificate(getSigningAlias(mdn.getPartnership(), partnershipType));
        } catch (CertificateNotFoundException cnfe)
        {
            cnfe.setPartnershipType(partnershipType);
            throw cnfe;
        }
    }

    public Map<String, X509Certificate> getCertificates() throws OpenAS2Exception
    {
          KeyStore ks = getKeyStore();

        try
        {
            Map<String, X509Certificate> certs = new HashMap<String, X509Certificate>();
            String certAlias;

            Enumeration<String> e = ks.aliases();

            while (e.hasMoreElements())
            {
                certAlias = e.nextElement();
                certs.put(certAlias, (X509Certificate) ks.getCertificate(certAlias));
            }

            return certs;
        } catch (GeneralSecurityException gse)
        {
            throw new WrappedException(gse);
        }
    }

    private int getRefreshInterval() throws InvalidParameterException
    {
        return getParameterInt(PARAM_INTERVAL, false);
    }

    public String getFilename() throws InvalidParameterException
    {
        return getParameter(PARAM_FILENAME, true);
    }

    public void setFilename(String filename)
    {
        getParameters().put(PARAM_FILENAME, filename);
    }

    public KeyStore getKeyStore()
    {
        return this.keyStore;
    }

    public void setKeyStore(KeyStore keyStore)
    {
        this.keyStore = keyStore;
    }

    public char[] getPassword() throws InvalidParameterException
    {
        return getParameter(PARAM_PASSWORD, true).toCharArray();
    }

    public void setPassword(char[] password)
    {
        getParameters().put(PARAM_PASSWORD, new String(password));
    }

    public PrivateKey getPrivateKey(X509Certificate cert) throws OpenAS2Exception
    {
        KeyStore ks = getKeyStore();
        String alias = null;

        try
        {
            alias = ks.getCertificateAlias(cert);
            logger.info("Alias found="+alias +"for cert"+cert. getSubjectDN()+","+cert.getIssuerDN()+","+cert.getSigAlgName());

            if (alias == null)
            {
                throw new KeyNotFoundException(cert, "-- alias null from getCertificateAlias(cert) call");
            }

            PrivateKey key = (PrivateKey) ks.getKey(alias, getPassword());

            if (key == null)
            {
                throw new KeyNotFoundException(cert, "-- key null from getKey(" + alias + ") call");
            }

            return key;
        } catch (GeneralSecurityException e)
        {
            throw new KeyNotFoundException(cert, alias, e);
        }
    }

    public PrivateKey getPrivateKey(String alias) throws OpenAS2Exception
    {
        KeyStore ks = getKeyStore();
        boolean isAliasFound=false;

        try
        {
            Enumeration e=ks.aliases();
            while(e.hasMoreElements())
            {
                String tempAlias=(String)e.nextElement();
                if(tempAlias.equalsIgnoreCase(alias))
                {
                    isAliasFound=true;
                    logger.info("Alias found with name "+alias);
                    break;
                }

            }
            //alias = ks.getCertificateAlias(cert);
            //logger.info("Alias found="+alias +"for cert"+cert. getSubjectDN()+","+cert.getIssuerDN()+","+cert.getSigAlgName());

            if (!isAliasFound)
            {
                throw new KeyNotFoundException(alias);
            }

            PrivateKey key = (PrivateKey) ks.getKey(alias, getPassword());


             if (key == null)

            {
                logger.info("Key not found for Alias name "+alias);
                throw new KeyNotFoundException(alias);
            }

            return key;
        } catch (GeneralSecurityException e)
        {
            throw new KeyNotFoundException( alias, e);
        }
    }

    public PrivateKey getPrivateKey(X509Certificate cert,String strPassword) throws OpenAS2Exception
    {
        KeyStore ks = getKeyStore();
        String alias = null;

        try
        {
            alias = ks.getCertificateAlias(cert);
            logger.info("Alias found="+alias +"for cert"+cert.getSubjectDN()+","+cert.getIssuerDN());

            if (alias == null)
            {
                throw new KeyNotFoundException(cert, "-- alias null from getCertificateAlias(cert) call");
            }

            PrivateKey key = (PrivateKey) ks.getKey(alias, strPassword.toCharArray());

            if (key == null)
            {
                throw new KeyNotFoundException(cert, "-- key null from getKey(" + alias + ") call");
            }

            return key;
        } catch (GeneralSecurityException e)
        {
            throw new KeyNotFoundException(cert, alias, e);
        }
    }

    public PrivateKey getPrivateKey(Message msg, X509Certificate cert) throws OpenAS2Exception
    {
        logger.info("MSG Passed is"+msg.getMessageID()+" and cert is "+cert.getSubjectDN()+","+cert.getSignature() );
        return getPrivateKey(cert);
    }

    public PrivateKey getPrivateKey(Message msg, String alias) throws OpenAS2Exception
    {
        logger.info("MSG Passed is"+msg.getMessageID()+" and Alias is "+alias );
        return getPrivateKey(alias);
    }

    public PrivateKey getPrivateKey(MessageMDN mdn, X509Certificate cert) throws OpenAS2Exception
    {
        return getPrivateKey(cert);
    }

    public PrivateKey getPrivateKey(MessageMDN mdn,String alias) throws OpenAS2Exception
    {
        return getPrivateKey(alias);
    }

    public Certificate[] getCertChain(String alias)  throws OpenAS2Exception
    {
        KeyStore ks = getKeyStore();

        try
        {
            if (!ks.containsAlias(alias))
            {
                throw new CertificateNotFoundException(null, alias);
            }

            Certificate[] certChain = ks.getCertificateChain(alias);
            if (certChain == null) {
                X509Certificate x509cert = (X509Certificate) ks.getCertificate(alias);
                if (x509cert.getSubjectDN().equals(x509cert.getIssuerDN()))
                {
                    // Trust chain is to itself
                    certChain = new X509Certificate[]{x509cert, x509cert};
                    if (logger.isInfoEnabled())
                    {
                        logger.info("Detected self-signed certificate and allowed import. Alias: " + alias);
                    }
                }
            }

            return certChain;
        } catch (GeneralSecurityException gse)
        {
            throw new WrappedException(gse);
        }
    }


    public void addCertificate(String alias, X509Certificate cert, boolean overwrite)
            throws OpenAS2Exception
    {
        KeyStore ks = getKeyStore();

        try
        {
            if (ks.containsAlias(alias) && !overwrite)
            {
                throw new CertificateExistsException(alias);
            }

            ks.setCertificateEntry(alias, cert);
            save(getFilename(), getPassword());
        } catch (GeneralSecurityException gse)
        {
            throw new WrappedException(gse);
        }
    }

    public void addPrivateKey(String alias, Key key, String password) throws OpenAS2Exception
    {
        KeyStore ks = getKeyStore();

        try
        {
            if (!ks.containsAlias(alias))
            {
                throw new CertificateNotFoundException(null, alias);
            }

            Certificate[] certChain = ks.getCertificateChain(alias);
            if (certChain == null)
            {
                X509Certificate x509cert = (X509Certificate) ks.getCertificate(alias);
                if (x509cert.getSubjectDN().equals(x509cert.getIssuerDN()))
                {
                    // Trust chain is to itself
                    certChain = new X509Certificate[]{x509cert, x509cert};
                    if (logger.isInfoEnabled())
                    {
                        logger.info("Detected self-signed certificate and allowed import. Alias: " + alias);
                    }
                }
            }
            ks.setKeyEntry(alias, key, password.toCharArray(), certChain);

            save(getFilename(), getPassword());
        } catch (GeneralSecurityException gse)
        {
            throw new WrappedException(gse);
        }
    }

    public void clearCertificates() throws OpenAS2Exception
    {
        KeyStore ks = getKeyStore();

        try
        {
            Enumeration<String> aliases = ks.aliases();

            while (aliases.hasMoreElements())
            {
                ks.deleteEntry(aliases.nextElement());
            }

            save(getFilename(), getPassword());
        } catch (GeneralSecurityException gse)
        {
            throw new WrappedException(gse);
        }
    }

    public void init(Session session, Map<String, String> options) throws OpenAS2Exception
    {
        super.init(session, options);

        // Override the password if it was passed as a system property
        String pwd = System.getProperty("org.openas2.cert.Password");
        if (pwd != null)
        {
            setPassword(pwd.toCharArray());
        }
        try
        {
            this.keyStore = AS2Util.getCryptoHelper().getKeyStore();
            if(this.keyStore!=null) {
                logger.info("keystore loaded during initialization");
            }
            else

            {
                logger.info("keystore not loaded during initialization");
            }

        } catch (Exception e)
        {
            throw new WrappedException(e);
        }
        load();
    }

    public void load(String filename, char[] password) throws OpenAS2Exception
    {
        try
        {
            FileInputStream fIn = new FileInputStream(filename);

            load(fIn, password);

            fIn.close();
        } catch (IOException ioe)
        {
            throw new WrappedException(ioe);
        }
    }

    public void load(InputStream in, char[] password) throws OpenAS2Exception
    {
        try
        {
            KeyStore ks = getKeyStore();

            synchronized (ks)
            {
                ks.load(in, password);
            }
        } catch (IOException ioe)
        {
            throw new WrappedException(ioe);
        } catch (GeneralSecurityException gse)
        {
            throw new WrappedException(gse);
        }
    }

    public void load() throws OpenAS2Exception
    {
        load(getFilename(), getPassword());
    }

    public void removeCertificate(X509Certificate cert) throws OpenAS2Exception
    {
        KeyStore ks = getKeyStore();

        try
        {
            String alias = ks.getCertificateAlias(cert);

            if (alias == null)
            {
                throw new CertificateNotFoundException(cert);
            }

            removeCertificate(alias);
        } catch (GeneralSecurityException gse)
        {
            throw new WrappedException(gse);
        }
    }

    public void removeCertificate(String alias) throws OpenAS2Exception
    {
        KeyStore ks = getKeyStore();

        try
        {
            if (ks.getCertificate(alias) == null)
            {
                throw new CertificateNotFoundException(null, alias);
            }

            ks.deleteEntry(alias);
            save(getFilename(), getPassword());
        } catch (GeneralSecurityException gse)
        {
            throw new WrappedException(gse);
        }
    }

    public void save() throws OpenAS2Exception
    {
        save(getFilename(), getPassword());
    }

    public void save(String filename, char[] password) throws OpenAS2Exception
    {
        try
        {
            FileOutputStream fOut = new FileOutputStream(filename, false);

            save(fOut, password);

            fOut.close();
        } catch (IOException ioe)
        {
            throw new WrappedException(ioe);
        }
    }

    public void save(OutputStream out, char[] password) throws OpenAS2Exception
    {
        try
        {
            getKeyStore().store(out, password);
        } catch (IOException ioe)
        {
            throw new WrappedException(ioe);
        } catch (GeneralSecurityException gse)
        {
            throw new WrappedException(gse);
        }
    }

    @Override
    public void schedule(ScheduledExecutorService executor) throws OpenAS2Exception
    {
        new FileMonitorAdapter() {
            @Override
            public void onConfigFileChanged() throws OpenAS2Exception
            {
                load();
                logger.info("- Certificates Reloaded -");
            }
        }.scheduleIfNeed(executor, new File(getFilename()), getRefreshInterval(), TimeUnit.SECONDS);
    }
}