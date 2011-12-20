package org.ccci.framework.cas;

import org.apache.http.cookie.Cookie;

public interface CasSession
{

    /**
     * Obtains a new session for this CAS client.  If a session already exists, it will be "forgotten."
     * 
     * @throws Exception
     */
    public abstract void obtainSession() throws Exception;

    /**
     * Determines if a session has been established (i.e. obtainSession() has been called successfully).
     * Note that the session might be expired or invalid even if this returns true.
     * 
     * @return
     */
    public abstract boolean isSessionEstablished();

    public abstract Cookie getSessionCookie();

}
