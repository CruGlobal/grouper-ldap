package org.ccci.idm.grouperldappc.old;

import java.net.URL;

import javax.xml.namespace.QName;

import org.apache.commons.logging.Log;
import org.ccci.idm.creds.client.CredentialsWebService;
import org.ccci.idm.creds.client.CredentialsWebServiceService;
import org.ccci.idm.grouper.dao.GrouperDao;
import org.ccci.idm.grouper.dao.GrouperDaoImpl;
import org.ccci.idm.grouper.obj.GrouperGroup;
import org.ccci.idm.ldap.Ldap;
import org.ccci.idm.obj.SsoUser;

import edu.internet2.middleware.grouper.SubjectFinder;
import edu.internet2.middleware.grouper.changeLog.provisioning.ChangeEvent;
import edu.internet2.middleware.grouper.changeLog.provisioning.EventProvisioningConnector;
import edu.internet2.middleware.grouper.util.ConfigItem;
import edu.internet2.middleware.grouper.util.GrouperUtil;
import edu.internet2.middleware.subject.Subject;

/**
 * This connector is designed specifically for our Cisco MeetingPlace needs.  It will create
 * and remove users in the Sun Directory Server based on their membership in a particular group
 * in Grouper.  (new membership = create account, remove membership = delete account)
 * 
 * This class can be used as a model for future systems that create accounts in external systems.
 * 
 * Note that it creates accounts with random passwords with the expectation that the Credentials
 * Storage and Synchronization System will be sending the real password over at a later time.
 * 
 * @author Nathan.Kopp
 *
 */
public class CiscoLdapUserAccountProvisioningConnector implements EventProvisioningConnector
{
    private static final Log LOG = GrouperUtil.getLog(CiscoLdapUserAccountProvisioningConnector.class);
    
    @ConfigItem
    private String userRdnAttribName = "cn";
    @ConfigItem
    private String userBaseDn = "ou=mpusers,dc=ccci,dc=org";
    @ConfigItem
    private String passwordAttribName = "userPassword";
    @ConfigItem
    private String usernameAttribName = "uid";
    @ConfigItem
    private String userLdapClasses = "inetOrgPerson,organizationalPerson,Person";
    
    @ConfigItem
    private String groupFullPath = "ccci:itroles:uscore:ciscoconferencing:users";
	@ConfigItem
	private String ldapUsername = "cn=Directory Manager";
	@ConfigItem
	private String ldapPassword = "---";
	@ConfigItem
	private String ldapUrl = "ldap://hart-a909.ccci.org:58389";
	
	@ConfigItem
    private String credsServiceWsdl = "https://cas.ccci.org/password-services/creds?wsdl";
	@ConfigItem
    private String credServiceClientId = "abc";
	@ConfigItem
    private String credServiceClientKey = "def";
	

    private GrouperDao dao;
    private CredentialsWebService service;

	public CiscoLdapUserAccountProvisioningConnector()
	{
		super();
	}

	public boolean dispatchEvent(ChangeEvent event) throws Exception
	{
		if (dao == null)
		{
			dao = new GrouperDaoImpl(null);
			CredentialsWebServiceService locator = new CredentialsWebServiceService(new URL(credsServiceWsdl), new QName("http://webservice.password.idm.ccci.org/", "CredentialsWebServiceService"));
            service = locator.getCredentialsWebServicePort();
		}

		if (event.getGroupName()!=null && event.getGroupName().equals(groupFullPath))
		{
		    LOG.debug("group: "+groupFullPath+" MATCHED");
		    
		    Ldap ldap = new Ldap(ldapUrl,ldapUsername,ldapPassword);
		    try
		    {
    			SsoUser user = dao.loadSsoUser(event.getSubjectId());
    			if (user == null)
                {
                    LOG.info("could not load user with subjectId: "+event.getSubjectId());
                    return false;
                }
    			
    			String userDn = userRdnAttribName+"="+user.getSsoGuid()+","+userBaseDn;
    			Subject grouperSubject = SubjectFinder.findById(event.getSubjectId(), false);
    			
    			GrouperGroup group = dao.loadGroup(event.getGroupName());
    			if(group==null)
                {
                    LOG.info("could not load group: "+event.getGroupName());
                    return false;
                }
    			
    			if (event.getEventType().equals(ChangeEvent.ChangeEventType.MEMBERSHIP_ADD.name()))
    			{
	                String lastName = grouperSubject.getAttributeValue("sn");
	                String firstName = grouperSubject.getAttributeValue("givenName");
	                //LOG.debug("grouperSubject attributes: "+grouperSubject.getAttributes());
	                String dummyPassword = ""+(long)(Math.random()*1000000000000L);
	                
	                LOG.debug("about to create user: "+firstName+","+lastName+","+user.getSsoGuid()+","+user.getUsername()+","+dummyPassword);
	                
                    String dn = userRdnAttribName+"="+user.getSsoGuid()+","+userBaseDn;
                    try
                    {
                        ldap.createEntity(dn, new String[]{userRdnAttribName, usernameAttribName, passwordAttribName,"sn","givenName","mail"}, new String[]{user.getSsoGuid(), user.getUsername(), dummyPassword, lastName, firstName, user.getUsername()}, userLdapClasses.split("\\s*,\\s*"));
                        LOG.debug("created user: "+firstName+","+lastName+","+user.getSsoGuid()+","+user.getUsername()+","+dummyPassword);
                    }
                    catch(javax.naming.NameAlreadyBoundException e)
                    {
                        LOG.info("user already exists: "+dn);
                    }
	                
	                service.syncCredentials(credServiceClientId, credServiceClientKey, user.getSsoGuid(), "cisco_sunds");
	                
	                LOG.debug("sent password to cisco_sunds for user: "+user.getSsoGuid()+","+user.getUsername());
    
    				return true;
    			}
    			else if (event.getEventType().equals(ChangeEvent.ChangeEventType.MEMBERSHIP_DELETE.name()))
    			{
    			    LOG.debug("about to delete user: "+userDn);
    			    
    			    ldap.deleteEntity(userDn);
    			    
    			    LOG.debug("deleted user: "+userDn);
    
    				return true;
    			}
		    }
		    finally
		    {
		        ldap.close();
		    }
		}

		return false;
	}

	public void close()
	{
		if (dao != null)
			dao.close();
		dao = null;
	}

	public void flush()
	{
	}

	public void init(String consumerName)
	{
	}
}
