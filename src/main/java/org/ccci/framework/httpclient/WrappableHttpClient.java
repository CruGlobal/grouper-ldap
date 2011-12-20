package org.ccci.framework.httpclient;

import org.apache.http.HttpEntity;
import org.apache.http.cookie.Cookie;


/**
 * 
 * @author nathan.kopp
 *
 */
public interface WrappableHttpClient
{
	public HttpEntity executeGet(String url) throws Exception;

    public Cookie getSessionCookie();
    public void setSessionCookie(Cookie sessionCookie);

}
