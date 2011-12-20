package org.ccci.idm.grouperstellentrecon.service;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.ccci.framework.cas.CasProtectedHttpClient;
import org.ccci.framework.cas.CasSession;
import org.ccci.framework.cas.CasSessionImpl;
import org.ccci.framework.httpclient.WrappableHttpClient;
import org.ccci.framework.httpclient.WrappableHttpClientInst;
import org.ccci.idm.grouperstellentrecon.process.DomDocument;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class StellentAccountService
{
    private String username;
    private String password;
    private String url;
    private String loginUrl;
    
    public StellentAccountService(String username, String password, String url, String loginUrl)
    {
        super();
        this.username = username;
        this.password = password;
        this.url = url;
        this.loginUrl = loginUrl;
    }

    public void close()
    {
    }

    public List<String> getAccountList() throws Exception
    {
        List<String> accounts = new ArrayList<String>();
        
        CasSession casSession = new CasSessionImpl(username, password, url, loginUrl, "modcasid");
        WrappableHttpClient client = CasProtectedHttpClient.wrapHttpClient(new WrappableHttpClientInst(), casSession);
        
        HttpEntity result = client.executeGet(url);
        DomDocument doc = new DomDocument(result.getContent());
        
        Node node = findNodeByName(doc, "#document");
        node = findNodeByName(doc, "SOAP-ENV:Envelope");
        node = findNodeByName(node, "SOAP-ENV:Body");
        node = findNodeByName(node, "idc:service");
        node = findNodeByName(node, "idc:document");
        node = findNodeByNameAndAttrib(node, "idc:resultset","name","DOCACCOUNT_INFO");
        
        for (int i = 0; i < node.getChildNodes().getLength(); i++)
        {
            Node childNode = node.getChildNodes().item(i);

            if (childNode.getNodeName().equals("idc:row"))
            {
                String account = childNode.getAttributes().getNamedItem("dDocAccount").getNodeValue();
                accounts.add(account);
            }
        }
        
        return accounts;
    }
    
    private Node findNodeByName(Node node, String name)
    {
        NodeList nodeList = node.getChildNodes();

        for (int i = 0; i < nodeList.getLength(); i++)
        {
            Node childNode = nodeList.item(i);

            if (childNode.getNodeName().equals(name))
            {
                return childNode;
            }
        }
        return null;
    }
    private Node findNodeByNameAndAttrib(Node node, String name, String attribName, String attribValue)
    {
        NodeList nodeList = node.getChildNodes();

        for (int i = 0; i < nodeList.getLength(); i++)
        {
            Node childNode = nodeList.item(i);

            if (childNode.getNodeName().equals(name))
            {
                Node attrib = childNode.getAttributes().getNamedItem(attribName);
                if(attrib!=null && attrib.getNodeValue().equals(attribValue)) return childNode;
            }
        }
        return null;
    }

}
