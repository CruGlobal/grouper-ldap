package org.ccci.framework.cas;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URLEncoder;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;

/**
 * This class manages a CAS session when interacting with an HTTP client protected by CAS.
 * 
 * Currently it only supports username & password login (as credentials to be passed to the
 * CAS server), but it could be expanded to support other methods of authentication,
 * such as certificates or SAML.
 * 
 * NOTE:  This requires the custom CCCI version of the CAS Server, which supports returning
 * data in the "CAS-Service" and "CAS-Ticket" HTTP headers.
 * 
 * @author Nathan.Kopp
 *
 */
public class CasSessionImpl implements CasSession
{
    private String casLogin;
    private String casPassword;
    private String defaultUrl;
    private String casUrl;
    private String cookieName;

    protected Cookie sessionCookie;
    
    /**
     * 
     * @param casLogin      The username to pass to CAS login.
     * @param casPassword   The password to pass to CAS login.
     * @param defaultUrl    The default URL that should be requested from the client when verifying the CAS service ticket.
     *                      This should represent a very small and fast read-only resource that everyone can access
     *                      but that is protected by the CAS client.  The goal of accessing this url is not the content
     *                      itself, but rather the process of the client validating the service ticket and sending a cookie.
     * @param casUrl        The URL for CAS login.  
     * @param cookieName    The name of the cookie that will be sent from the client, which we must track.
     */
    public CasSessionImpl(String casLogin, String casPassword, String defaultUrl, String casUrl, String cookieName)
    {
        super();
        this.casLogin = casLogin;
        this.casPassword = casPassword;
        this.defaultUrl = defaultUrl;
        this.casUrl = casUrl;
        this.cookieName = cookieName;
    }

    /* (non-Javadoc)
     * @see org.ccci.framework.cas.CasSession#obtainSession()
     */
    public void obtainSession() throws Exception
    {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        
//        System.out.println(buildDefaultRequestToCasServer());
        HttpGet getServiceTicket = new HttpGet(buildDefaultRequestToCasServer());   //construct a generic get to retrieve ST
        setFollowRedirect(getServiceTicket, false);
        
//        System.out.println("start getServiceTicket");
        HttpResponse responseFromCasServer = httpClient.execute(getServiceTicket);
//        System.out.println("end getServiceTicket");

        Header[] servHeader = responseFromCasServer.getHeaders("CAS-Service");  //get the service header
        Header[] stHeader = responseFromCasServer.getHeaders("CAS-Ticket");     //get the service ticket header

        if(servHeader == null || stHeader == null) throw new RuntimeException("Unable to get Service Ticket from CAS Server");

        String service = defaultUrl; //servHeader[0].getValue();
        String serviceTicket = stHeader[0].getValue();
        
//        System.out.println("service: "+service);
//        System.out.println("serviceTicket: "+serviceTicket);

        getServiceTicket.abort();   //abort get request to release the connection in HttpClient

        //construct a GET with the generic service and ST obtained from the CAS server to obtain a cookie
        //System.out.println(buildDefaultRequestWithServiceTicket(service,serviceTicket));
        HttpGet getCookie = new HttpGet(buildDefaultRequestWithServiceTicket(service,serviceTicket));
        setFollowRedirect(getCookie, false);

//        System.out.println("start getCookie");
        HttpResponse responseWithCookie = httpClient.execute(getCookie);
//        System.out.println("end getCookie");
        
//        InputStream is = responseWithCookie.getEntity().getContent();
//        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
//        String line = reader.readLine();
//        while(line!=null)
//        {
//            System.out.println(line);
//            line = reader.readLine();
//        }

        sessionCookie = extractCookieFromResponse(responseWithCookie);   //go get the cookie and store it
        getCookie.abort(); //abort get request to release the connection in HttpClient
        
        if(sessionCookie == null) throw new RuntimeException("Unable to get Cookie from CAS Client");
        
        return;
    }

    /* (non-Javadoc)
     * @see org.ccci.framework.cas.CasSession#isSessionEstablished()
     */
    public boolean isSessionEstablished()
    {
        return sessionCookie!=null;
    }

    /** 
     * Build a request to the CAS Server using the "default URL" as the service.
     * Also, it's necessary to send along the username and password for authentication
     * The CAS Server will respond with a Service Ticket.
     * 
     * 
     * @param httpClient
     * @param casUri
     * @return
     * @throws Exception
     */
    private URI buildDefaultRequestToCasServer() throws Exception
    {
        StringBuilder uri = new StringBuilder("");
        uri.append(casUrl);
        uri.append("?service=");
        uri.append(URLEncoder.encode(defaultUrl, "UTF-8"));
        uri.append("&username=");
        uri.append(casLogin);
        uri.append("&password=");
        uri.append(casPassword);
        return new URI(uri.toString());
    }
    
    private static void setFollowRedirect(HttpGet get, boolean followRedirects)
    {
        HttpParams params = new BasicHttpParams();
        params.setParameter(ClientPNames.HANDLE_REDIRECTS, followRedirects);
        get.setParams(params);
        return;
    }

    /**
     * Build a URI with the service previously used in the request to the CAS Server and the 
     * Service Ticket sent from the CAS Server
     * @param service
     * @param serviceTicket
     * @return
     * @throws Exception
     */
    private static URI buildDefaultRequestWithServiceTicket(String service, String serviceTicket) throws Exception
    {
        StringBuilder uri = new StringBuilder("");
        uri.append(service);
        if(service.contains("?")) uri.append("&ticket=");
        else uri.append("?ticket=");
        uri.append(serviceTicket);

        return new URI(uri.toString());
    }
    
    /**
     * The CAS Client now has responded with a cookie in the headers that we need to go find.
     * 
     * @param response
     * @return
     */
    private Cookie extractCookieFromResponse(HttpResponse response)
    {
        Header[] headers = response.getAllHeaders();
        for(int i=0; i < headers.length; i++)
        {
            if("Set-Cookie".equals(headers[i].getName()))
            {
                String headerValue = headers[i].getValue();
                int startIndex = headerValue.indexOf('=');
                int endIndex = headerValue.indexOf(';');
                String cookieName = headerValue.substring(0,startIndex);
                if(cookieName.equals(this.cookieName))
                {
                    String cookie = headerValue.substring(startIndex+1, endIndex);
                    return new BasicClientCookie(cookieName,cookie);
                }
            }
        }
        return null;
    }

    /* (non-Javadoc)
     * @see org.ccci.framework.cas.CasSession#getSessionCookie()
     */
    public Cookie getSessionCookie()
    {
        return sessionCookie;
    }

}
