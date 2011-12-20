package org.ccci.framework.httpclient;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
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
    public Cookie sessionCookie;
    
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
		else
		{
		    throw new RuntimeException("Session not yet established");
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
}
