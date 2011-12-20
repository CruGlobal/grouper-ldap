package org.ccci.idm.grouperstellentrecon.service;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.ccci.dom.DomDocument;
import org.ccci.dom.DomNode;
import org.ccci.framework.cas.CasProtectedHttpClient;
import org.ccci.framework.cas.CasSession;
import org.ccci.framework.cas.CasSessionImpl;
import org.ccci.framework.httpclient.WrappableHttpClient;
import org.ccci.framework.httpclient.WrappableHttpClientInst;
import org.w3c.dom.Node;

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
        
        DomNode node = doc.getFirstNodeByName("SOAP-ENV:Envelope");
        node = node.getFirstNodeByName("SOAP-ENV:Body");
        node = node.getFirstNodeByName("idc:service");
        node = node.getFirstNodeByName("idc:document");
        node = node.getFirstNodeByNameAndAttrib("idc:resultset","name","DOCACCOUNT_INFO");
        
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

}
