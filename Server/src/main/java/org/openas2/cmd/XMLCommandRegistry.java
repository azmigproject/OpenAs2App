package org.openas2.cmd;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.openas2.OpenAS2Exception;
import org.openas2.Session;
import org.openas2.WrappedException;
import org.openas2.XMLSession;
import org.openas2.util.XMLUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.openas2.lib.dbUtils.*;


public class XMLCommandRegistry extends BaseCommandRegistry {
    public static final String PARAM_FILENAME = "filename";
    public Map<String, Object>  GetMultiCommands(){return _multiCommands;}
    public void  setMultiCommands(Map<String, Object> multiCommands ) {this._multiCommands=multiCommands;}
    private Map<String, Object>_multiCommands=null;
    public void init(Session session, Map<String, String> parameters) throws OpenAS2Exception
    {
        super.init(session, parameters);
        if(_multiCommands!=null) {
            refresh(_multiCommands);
        }
        else

        {
            refresh();
        }
    }



    public void load(InputStream in)
            throws ParserConfigurationException, SAXException, IOException, OpenAS2Exception
    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        DocumentBuilder parser = factory.newDocumentBuilder();
        Document document = parser.parse(in);
        Element root = document.getDocumentElement();
        NodeList rootNodes = root.getChildNodes();
        Node rootNode;
        String nodeName;

        getCommands().clear();

        for (int i = 0; i < rootNodes.getLength(); i++)
        {
            rootNode = rootNodes.item(i);

            nodeName = rootNode.getNodeName();

            if (nodeName.equals("command"))
            {
                loadCommand(rootNode, null);
            } else if (nodeName.equals("multicommand"))
            {
                loadMultiCommand(rootNode, null);
            }
        }
    }

    public void refresh(Map<String, Object> multicommands) throws OpenAS2Exception
    {
        try
        {
            for(Map.Entry<String, Object> entry : multicommands.entrySet()) {

                org.openas2.lib.dbUtils.Multicommand[] multcmds = (org.openas2.lib.dbUtils.Multicommand[])entry.getValue();
                for(int count=0; count<multcmds.length;count++)
                {

                    loadMultiCommand(multcmds[count]);
                }
            }
        } catch (Exception e)
        {
            throw new WrappedException(e);
        }
    }

    protected void loadCommand( org.openas2.lib.dbUtils.Command command, MultiCommand parent)
            throws OpenAS2Exception
    {
        Map<String, String> parameters =  new HashMap<String, String>();
        parameters.put("classname", command.getClassName());

        Command cmd = (Command) XMLUtil.getComponent(command.getClassName(),parameters, (XMLSession) getSession());

        if (parent != null)
        {
            parent.getCommands().add(cmd);
        } else
        {
            getCommands().add(cmd);
        }


    }

    protected void loadMultiCommand(org.openas2.lib.dbUtils.Multicommand multCmd)
            throws OpenAS2Exception
    {
        MultiCommand cmd = new MultiCommand();
        Map<String, String> parameters =  new HashMap<String, String>();
        parameters.put("name", multCmd.getName());
        parameters.put("description", multCmd.getDescription());
        cmd.init(getSession(),parameters);


            getCommands().add(cmd);

        org.openas2.lib.dbUtils.Command[] childCmd=multCmd.getCommands();
        if(childCmd!=null && childCmd.length>0) {
            for (int i = 0; i < childCmd.length;
            i++)
            {
                loadCommand(childCmd[i],cmd);
            }
        }
    }

    public void refresh() throws OpenAS2Exception
    {
        try
        {
            load(new FileInputStream(getParameter(PARAM_FILENAME, true)));
        } catch (Exception e)
        {
            throw new WrappedException(e);
        }
    }

    protected void loadCommand(Node node, MultiCommand parent)
            throws OpenAS2Exception
    {
        Command cmd = (Command) XMLUtil.getComponent(node, (XMLSession) getSession());

        if (parent != null)
        {
            parent.getCommands().add(cmd);
        } else
        {
            getCommands().add(cmd);
        }


    }

    protected void loadMultiCommand(Node node, MultiCommand parent)
            throws OpenAS2Exception
    {
        MultiCommand cmd = new MultiCommand();
        cmd.init(getSession(), XMLUtil.mapAttributes(node));

        if (parent != null)
        {
            parent.getCommands().add(cmd);
        } else
        {
            getCommands().add(cmd);
        }

        NodeList childCmds = node.getChildNodes();

        Node childNode;
        String childName;

        for (int i = 0; i < childCmds.getLength(); i++)
        {
            childNode = childCmds.item(i);

            childName = childNode.getNodeName();

            if (childName.equals("command"))
            {
                loadCommand(childNode, cmd);
            } else if (childName.equals("multicommand"))
            {
                loadMultiCommand(childNode, cmd);
            }
        }
    }
}
