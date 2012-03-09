package org.ccci.idm.grouperldappc;

import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.ccci.idm.grouper.dao.GrouperDao;
import org.ccci.idm.grouper.dao.GrouperDaoImpl;
import org.ccci.idm.grouper.obj.GrouperGroup;
import org.ccci.idm.ldap.Ldap;
import org.ccci.idm.obj.SsoUser;

import edu.internet2.middleware.grouper.changeLog.provisioning.ChangeEvent;
import edu.internet2.middleware.grouper.changeLog.provisioning.EventProvisioningConnector;
import edu.internet2.middleware.grouper.util.ConfigItem;
import edu.internet2.middleware.grouper.util.GrouperUtil;

/**
 * This connector provides a one-way sync from one stem in grouper to one base DN in LDAP.
 * It will create and delete groups in LDAP to correspond to groups in the specified stem,
 * and will manage membership in LDAP to correspond to Grouper membership.
 * 
 * @author Nathan.Kopp
 *
 */
public class NonRecursiveLdapGroupConnector implements EventProvisioningConnector
{
    private static final Log LOG = GrouperUtil.getLog(NonRecursiveLdapGroupConnector.class);
    
    @ConfigItem
    private String userRdnAttribName = "cn";
    @ConfigItem
    private String userBaseDn = "cn=sso,dc=mygcx,dc=org";
    @ConfigItem
    private String groupRdnAttrib = "cn";
    @ConfigItem
    private String groupBaseDn = "ou=Roles,ou=Stellent,ou=Xellerate Users,cn=sso,dc=mygcx,dc=org";
    @ConfigItem
    private String groupLdapClasses = "groupOfUniqueNames";
    @ConfigItem
    private String grouperPrefix = "ccci:itroles:uscore:stellent:roles";
	@ConfigItem
	private String ldapUsername = "cn=orcladmin,cn=Users,dc=mygcx,dc=org";
	@ConfigItem
	private String ldapPassword = "---";
	@ConfigItem
	private String ldapUrl = "ldap://hart-a935.ccci.org:389";

    private GrouperDao dao;

	public NonRecursiveLdapGroupConnector()
	{
		super();
		LOG.debug("Constructed NonRecursiveLdapGroupConnector");
	}

	public boolean dispatchEvent(ChangeEvent event) throws Exception
	{
		String groupName = null;
		if(isMembershipEvent(event)) groupName = event.getGroupName();
		else groupName = event.getName();

		if (groupName!=null && groupName.startsWith(grouperPrefix))
		{
		    LOG.debug("group: "+groupName+" MATCHED PREFIX");
	        createDaoIfNecessary();
	        
            Ldap ldap = new Ldap(ldapUrl,ldapUsername,ldapPassword);
            
            try
            {
                GrouperGroup group = dao.loadGroup(groupName);
                if(group==null)
                {
                    LOG.info("could not load group: "+groupName);
                    return false;
                }
                String groupDn = groupRdnAttrib+"="+group.getId()+","+groupBaseDn;
                
    		    if(isMembershipEvent(event))
    		    {
        			SsoUser user = dao.loadSsoUser(event.getSubjectId());
        			if (user == null)
        			{
        			    LOG.info("could not load user with subjectId: "+event.getSubjectId());
        			    return false;
        			}
        			String userDn = userRdnAttribName+"="+user.getSsoGuid()+","+userBaseDn;
        			
        			try
        			{
            			if (event.getEventType().equals(ChangeEvent.ChangeEventType.MEMBERSHIP_ADD.name()))
            			{
            			    LOG.debug("adding user to group: "+userDn+" "+groupDn);
            			    ldap.addUserToGroup(groupDn, userDn);
            			}
            			else if (event.getEventType().equals(ChangeEvent.ChangeEventType.MEMBERSHIP_DELETE.name()))
            			{
            			    LOG.debug("removing user from group: "+userDn+" "+groupDn);
            			    ldap.removeUserFromGroup(groupDn, userDn);
            			}
            			else
            			{
            			    LOG.error("event type not recognized");
            			}
        			}
        			catch(Exception e)
        			{
        			    if(e instanceof javax.naming.directory.AttributeInUseException)
                        {
                            LOG.info(e.getMessage());
                        }
                        else
                        {
                            LOG.error(e);
                        }
        			}
        			return true;
    		    }
    		    else if(event.getEventType().equals(ChangeEvent.ChangeEventType.GROUP_ADD.name()))
                {
    		        try
    		        {
    		            LOG.debug("adding group: "+groupDn);
    		            createGroupInLdap(ldap, group, groupDn);
    		        }
    		        catch(javax.naming.NameAlreadyBoundException e)
    		        {
    		            LOG.error(groupDn+" already exists in "+ldapUrl);
    		        }
    		        catch(Exception e)
    		        {
    		            if(e.getMessage().contains("already exists"))
                        {
                            LOG.info(e.getMessage());
                        }
                        else
                        {
                            LOG.error(e);
                        }
    		        }
    		        return true;
                }
    		    else if(event.getEventType().equals(ChangeEvent.ChangeEventType.GROUP_DELETE.name()))
    		    {
    		        try
                    {
    		            LOG.debug("removing group: "+groupDn);
    		            removeGroupFromLdap(ldap, groupDn);
                    }
    		        catch(Exception e)
                    {
    		            LOG.error(e);
                    }
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

    private boolean isMembershipEvent(ChangeEvent event)
    {
        return event.getEventType().equals(ChangeEvent.ChangeEventType.MEMBERSHIP_ADD.name()) || event.getEventType().equals(ChangeEvent.ChangeEventType.MEMBERSHIP_DELETE.name());
    }

    private void createDaoIfNecessary()
    {
        if (dao == null)
		{
			dao = new GrouperDaoImpl(null);
		}
    }

    private void removeGroupFromLdap(Ldap ldap, String groupDn) throws NamingException
    {
        ldap.deleteEntity(groupDn);
    }

    private void createGroupInLdap(Ldap ldap, GrouperGroup group, String groupDn) throws NamingException
    {
        String[] attribNames = new String[] { groupRdnAttrib };
        String[] attribValues = new String[] { group.getId() };
        String[] ldapClasses = groupLdapClasses.split("\\s*,\\s*");
        ldap.createEntity(groupDn, attribNames, attribValues, ldapClasses);
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
	    if(grouperPrefix.endsWith(":")) grouperPrefix = grouperPrefix+":";
	}
}
