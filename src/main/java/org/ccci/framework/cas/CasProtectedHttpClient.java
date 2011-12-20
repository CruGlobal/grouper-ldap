package org.ccci.framework.cas;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.ccci.framework.httpclient.WrappableHttpClient;


/**
 * This is designed to wrap a JAX-WS or JAX-RPC client to provide CAS-based authentication.
 * It will automatically establish a session using CAS and will also detect when a session
 * needs to be reestablished (due to expiration or invalidation).
 * 
 * This class is a blend of the factory pattern and one component of the underlying
 * implementation (the InvocationHandler).
 * 
 * @author Nathan.Kopp
 *
 */
public class CasProtectedHttpClient implements java.lang.reflect.InvocationHandler
{
    /**
     * This is the proxied service
     */
    private WrappableHttpClient service;
    
    /**
     * This is the CAS session for the proxied service
     */
    private CasSession casSession;

    /**
     * Wrap a web service for CAS authentication.
     * 
     * @param obj
     * @param casSession
     * @return
     */
    public static WrappableHttpClient wrapHttpClient(WrappableHttpClient obj, CasSession casSession)
    {
        return (WrappableHttpClient)java.lang.reflect.Proxy.newProxyInstance(obj.getClass().getClassLoader(),
            obj.getClass().getInterfaces(), new CasProtectedHttpClient(obj, casSession));
    }

    /**
     * Private InvocationHandler constructor.
     * 
     * @param obj
     * @param casSession
     */
    private CasProtectedHttpClient(WrappableHttpClient obj, CasSession casSession)
    {
        this.service = obj;
        this.casSession = casSession;
    }

    /**
     * The main entrance point for the invocation handler.
     */
    public Object invoke(Object proxy, Method m, Object[] args) throws Throwable
    {
        if (!casSession.isSessionEstablished()) obtainSession();
        else applySession();

        try
        {
            return invokeUnwrapInvoctationTargetException(service, m, args);
        }
        catch (Throwable t)
        {
            obtainSession();
            return invokeUnwrapInvoctationTargetException(service, m, args);
        }
    }
    
    
    /**
     * Stupid Method.invoke() shouldn't wrap the exception... it just makes life difficult... so we'll unwrap it.
     * 
     * NOTE that the arguments for this method are NOT the same as those of InvocationHandler.invoke, even though
     * they might look the same
     * 
     * @param obj   the instance of the object on which to invoke the method
     * @param m     the method to invoke
     * @param args  the method arguments
     * @return
     * @throws Throwable
     */
    private static Object invokeUnwrapInvoctationTargetException(Object obj, Method m, Object[] args) throws Throwable
    {
        try
        {
            return m.invoke(obj, args);
        }
        catch (InvocationTargetException e)
        {
            throw e.getTargetException();
        }
    }

    /**
     * Obtain a session and store it in the web service's requestContext
     * 
     * @throws Exception
     */
    private void obtainSession() throws Exception
    {
        long start = System.currentTimeMillis();
        casSession.obtainSession();
        applySession();
        long end = System.currentTimeMillis();
        System.out.println("Time for obtainSession for HttpClient: "+(end-start));
    }

    private void applySession()
    {
        service.setSessionCookie(casSession.getSessionCookie());
    }

}
