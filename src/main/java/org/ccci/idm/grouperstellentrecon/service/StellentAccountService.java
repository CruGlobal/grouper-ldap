package org.ccci.idm.grouperstellentrecon.service;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.ccci.dom.DomDocument;
import org.ccci.dom.DomNode;
import org.ccci.framework.BasicProtectedHttpClient;
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
    private boolean useCas;
    
    public StellentAccountService(String username, String password, String url, String loginUrl, boolean useCas)
    {
        super();
        this.username = username;
        this.password = password;
        this.url = url;
        this.loginUrl = loginUrl;
        this.useCas = useCas;
    }

    public void close()
    {
    }

    public List<String> getAccountList() throws Exception
    {
        System.out.println("ReconcileAccounts getAccountList");
        List<String> accounts = new ArrayList<String>();
        
        CasSession casSession = new CasSessionImpl(username, password, url, loginUrl, "modcasid");
        WrappableHttpClient client = null;
        if(useCas) client = CasProtectedHttpClient.wrapHttpClient(new WrappableHttpClientInst(), casSession);
        else client = BasicProtectedHttpClient.wrapHttpClient(new WrappableHttpClientInst(), username, password);
        
        HttpEntity result = client.executeGet(url);
        /*
        int c;
        while((c = result.getContent().read()) >= 0)
        {
            System.out.print((char)c);
        }
        */
        DomDocument doc = null;
        try
        {
            doc = new DomDocument(result.getContent());
        }
        catch(org.xml.sax.SAXParseException e)
        {
            System.out.println("parsing error occurred for URL: "+url);
            HttpEntity result2 = client.executeGet(url);
            int c;
            while((c = result2.getContent().read()) >= 0)
            {
                System.out.print((char)c);
            }
            throw e;
        }
        
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
