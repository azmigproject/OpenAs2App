package org.openas2.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.openas2.Component;
import org.openas2.OpenAS2Exception;
import org.openas2.Session;
import org.openas2.WrappedException;
import org.openas2.lib.dbUtils.*;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class XMLUtil {
    public static Component getComponent(Node node, Session session)
            throws OpenAS2Exception
    {
        Node classNameNode = node.getAttributes().getNamedItem("classname");

        if (classNameNode == null)
        {
            throw new OpenAS2Exception("Missing classname");
        }

        String className = classNameNode.getNodeValue();

        try
        {
            Class<?> objClass = Class.forName(className);

            if (!Component.class.isAssignableFrom(objClass))
            {
                throw new OpenAS2Exception("Class " + className + " must implement " +
                        Component.class.getName());
            }

            Component obj = (Component) objClass.newInstance();

            Map<String, String> parameters = XMLUtil.mapAttributes(node);

            updateDirectories(session.getBaseDirectory(), parameters);

            obj.init(session, parameters);

            return obj;
        } catch (Exception e)
        {
            throw new WrappedException("Error creating component: " + className, e);
        }
    }


    public static Component getComponent(String className, Map<String,String>mapParms, Session session)
            throws OpenAS2Exception
    {
        try
        {
            Class<?> objClass = Class.forName(className);

            if (!Component.class.isAssignableFrom(objClass))
            {
                throw new OpenAS2Exception("Class " + className + " must implement " +
                        Component.class.getName());
            }

            Component obj = (Component) objClass.newInstance();

            Map<String, String> parameters = XMLUtil.mapAttributes(mapParms,true);

            updateDirectories(session.getBaseDirectory(), parameters);

            obj.init(session, parameters);

            return obj;
        } catch (Exception e)
        {
            throw new WrappedException("Error creating component: " + className, e);
        }
    }

    public static Component getCommandComponent(String className, Map<String,String>mapParms, Map<String, Object> cmdList, Session session)
            throws OpenAS2Exception
    {
        try
        {
            Class<?> objClass = Class.forName(className);

            if (!Component.class.isAssignableFrom(objClass))
            {
                throw new OpenAS2Exception("Class " + className + " must implement " +
                        Component.class.getName());
            }

            org.openas2.cmd.XMLCommandRegistry obj = (org.openas2.cmd.XMLCommandRegistry) objClass.newInstance();

            Map<String, String> parameters = XMLUtil.mapAttributes(mapParms,true);

            updateDirectories(session.getBaseDirectory(), parameters);
            obj.setMultiCommands(cmdList);
            obj.init(session, parameters);

            return obj;
        } catch (Exception e)
        {
            throw new WrappedException("Error creating component: " + className, e);
        }
    }

    public static Component getPartnerShipComponent(String className, Map<String,String>mapParms, List<partner> partnerList, Profile companyProfile,List<Profile> profileList, ServersSettings serversSettings,   Session session)
            throws OpenAS2Exception
    {
        try
        {
            Class<?> objClass = Class.forName(className);

            if (!Component.class.isAssignableFrom(objClass))
            {
                throw new OpenAS2Exception("Class " + className + " must implement " +
                        Component.class.getName());
            }

            org.openas2.partner.XMLPartnershipFactory obj = (org.openas2.partner.XMLPartnershipFactory) objClass.newInstance();

            Map<String, String> parameters = XMLUtil.mapAttributes(mapParms,true);
            updateDirectories(session.getBaseDirectory(), parameters);
            obj.setPartnersFromDB(partnerList);
            obj.setProfileFromDB(profileList);
            obj.setCompanyProfile(companyProfile);
            obj.setServerSettings(serversSettings);
            obj.init(session, parameters);

            return obj;
        } catch (Exception e)
        {
            throw new WrappedException("Error creating component: " + className, e);
        }
    }

    public static Node findChildNode(Node parent, String childName)
    {
        NodeList childNodes = parent.getChildNodes();
        int childCount = childNodes.getLength();
        Node child;

        for (int i = 0; i < childCount; i++)
        {
            child = childNodes.item(i);

            if (child.getNodeName().equals(childName))
            {
                return child;
            }
        }

        return null;
    }

    public static Map<String, String> mapAttributeNodes(NodeList nodes, String nodeName, String nodeKeyName,
                                                        String nodeValueName) throws OpenAS2Exception
    {
        Map<String, String> attributes = new HashMap<String, String>();
        int nodeCount = nodes.getLength();
        Node attrNode;
        NamedNodeMap nodeAttributes;
        Node tmpNode;
        String attrName;
        String attrValue;

        for (int i = 0; i < nodeCount; i++)
        {
            attrNode = nodes.item(i);

            if (attrNode.getNodeName().equals(nodeName))
            {
                nodeAttributes = attrNode.getAttributes();
                tmpNode = nodeAttributes.getNamedItem(nodeKeyName);

                if (tmpNode == null)
                {
                    throw new OpenAS2Exception(attrNode.toString() +
                            " does not have key attribute: " + nodeKeyName);
                }

                attrName = tmpNode.getNodeValue();
                tmpNode = nodeAttributes.getNamedItem(nodeValueName);

                if (tmpNode == null)
                {
                    throw new OpenAS2Exception(attrNode.toString() +
                            " does not have value attribute: " + nodeValueName);
                }

                attrValue = tmpNode.getNodeValue();
                attributes.put(attrName, attrValue);
            }
        }

        return attributes;
    }

    public static Map<String, String> mapAttributes(Node node, boolean keyToLowerCase)
    {
        Map<String, String> attrMap = new HashMap<String, String>();
        NamedNodeMap attrNodes = node.getAttributes();
        int attrCount = attrNodes.getLength();
        Node attribute;

        for (int i = 0; i < attrCount; i++)
        {
            attribute = attrNodes.item(i);
            String key = attribute.getNodeName();
            if (keyToLowerCase) key = key.toLowerCase();
            attrMap.put(key, attribute.getNodeValue());
        }

        return attrMap;
    }

    public static Map<String, String> mapAttributes(Map<String, String> attributeList, boolean keyToLowerCase)
    {
        Map<String, String> attrMap = new HashMap<String, String>();

        for(Map.Entry<String, String> entry : attributeList.entrySet()) {
        String key = entry.getKey();
            String parmValue = entry.getValue();

            if (keyToLowerCase) key = key.toLowerCase();
            attrMap.put(key, parmValue);
        }

        return attrMap;
    }

    public static Map<String, String> mapAttributes(Node node)
    {
    	return mapAttributes(node, true);
    }
    public static Map<String, String> mapAttributes(Map<String, String> attributeList, String[] requiredAttributes)
            throws OpenAS2Exception
    {
        Map<String, String> attributes = mapAttributes(attributeList,true);
        String attrName;

        for (String requiredAttribute : requiredAttributes)
        {
            attrName = requiredAttribute;

            if (attributes.get(attrName) == null)
            {
                throw new OpenAS2Exception("AttributeList is missing required attribute: " +
                        attrName);
            }
        }

        return attributes;
    }

    public static Map<String, String> mapAttributes(Node node, String[] requiredAttributes)
            throws OpenAS2Exception
    {
        Map<String, String> attributes = mapAttributes(node);
        String attrName;

        for (String requiredAttribute : requiredAttributes)
        {
            attrName = requiredAttribute;

            if (attributes.get(attrName) == null)
            {
                throw new OpenAS2Exception(node.toString() + " is missing required attribute: " +
                        attrName);
            }
        }

        return attributes;
    }

    private static void updateDirectories(String baseDirectory, Map<String, String> attributes)
            throws OpenAS2Exception
    {
        Iterator<Entry<String, String>> attrIt = attributes.entrySet().iterator();
        Map.Entry<String, String> attrEntry;
        String value;

        while (attrIt.hasNext())
        {
            attrEntry = attrIt.next();
            value = attrEntry.getValue();

            if (value.startsWith("%home%"))
            {
                if (baseDirectory != null)
                {
                    value = baseDirectory + value.substring(6);
                    attributes.put(attrEntry.getKey(), value);
                } else
                {
                    throw new OpenAS2Exception("Base directory isn't set");
                }
            }
        }
    }
}
