package org.ccci.framework;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.ccci.framework.httpclient.WrappableHttpClient;


/**
 * This class is a blend of the factory pattern and one component of the underlying
 * implementation (the InvocationHandler).
 * 
 * @author Nathan.Kopp
 *
 */
public class BasicProtectedHttpClient implements java.lang.reflect.InvocationHandler
{
    /**
     * This is the proxied service
     */
    private WrappableHttpClient service;
    
    private String username;
    private String password;

    /**
     * Wrap a web service for CAS authentication.
     * 
     * @param obj
     * @param casSession
     * @return
     */
    public static WrappableHttpClient wrapHttpClient(WrappableHttpClient obj, String username, String password)
    {
        return (WrappableHttpClient)java.lang.reflect.Proxy.newProxyInstance(obj.getClass().getClassLoader(),
            obj.getClass().getInterfaces(), new BasicProtectedHttpClient(obj, username, password));
    }

    /**
     * Private InvocationHandler constructor.
     * 
     * @param obj
     * @param casSession
     */
    private BasicProtectedHttpClient(WrappableHttpClient obj, String username, String password)
    {
        this.service = obj;
        this.username = username;
        this.password = password;
    }

    /**
     * The main entrance point for the invocation handler.
     */
    public Object invoke(Object proxy, Method m, Object[] args) throws Throwable
    {
        applyBasicAuth();

        return invokeUnwrapInvoctationTargetException(service, m, args);
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

    private void applyBasicAuth()
    {
        service.setBasicAuth(username, password);
    }

}
