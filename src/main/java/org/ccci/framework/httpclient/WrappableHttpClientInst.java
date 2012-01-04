package org.ccci.framework.httpclient;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.auth.params.AuthPNames;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.AuthPolicy;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.DefaultRedirectHandler;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;


/**
 * 
 * @author nathan.kopp
 *
 */
public class WrappableHttpClientInst implements WrappableHttpClient
{
    private Cookie sessionCookie;
    private UsernamePasswordCredentials creds;
    
	/**
	 * Request content from Stellent for the Designation passed in.
	 * This method is the entry point and is setup to handle redirects
	 * to the CAS Server and such if necessary.
	 * @param designation
	 */
	public HttpEntity executeGet(String url) throws Exception
	{
		DefaultHttpClient httpClient = new DefaultHttpClient();
		HttpGet get = new HttpGet(url);
		HttpContext context = new BasicHttpContext();
		DefaultRedirectHandler redirectHandler = new DefaultRedirectHandler();
		HttpParams params = new BasicHttpParams();

		HttpResponse response = null;

		params.setParameter(ClientPNames.HANDLE_REDIRECTS, false);
		get.setParams(params);

		if(sessionCookie != null)
		{
			get.addHeader("Cookie", sessionCookie.getName()+"="+sessionCookie.getValue());		//add the cookie
		}
		
		if(creds!=null)
		{
		    List<String> authpref = new ArrayList<String>();
		    authpref.add(AuthPolicy.BASIC);
		    authpref.add(AuthPolicy.DIGEST);
		    httpClient.getParams().setParameter(AuthPNames.PROXY_AUTH_PREF, authpref);
		    
		    URL parsedUrl = new URL(url);
		    
		    httpClient.getCredentialsProvider().setCredentials(
		        new AuthScope(parsedUrl.getHost(), parsedUrl.getPort()), 
		        new UsernamePasswordCredentials(creds.getUserName(), creds.getPassword()));
		}
		
		
		response = httpClient.execute(get,context);

		if(redirectHandler.isRedirectRequested(response, context) &&
		        redirectHandler.getLocationURI(response, context).toString().contains("login"))
		{
			get.abort();	//abort get request to release the connection in HttpClient
			throw new RuntimeException("Session not yet established");
		}
		else
		{
			return response.getEntity();
		}
	}

    public Cookie getSessionCookie()
    {
        return sessionCookie;
    }

    public void setSessionCookie(Cookie sessionCookie)
    {
        this.sessionCookie = sessionCookie;
    }

    @Override
    public void setBasicAuth(String username, String password)
    {
        creds = new UsernamePasswordCredentials(username, password);
    }
}
