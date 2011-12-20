package org.ccci.idm.oidpasswordpolicy;

import org.ccci.idm.grouper.dao.GrouperDao;
import org.ccci.idm.grouper.dao.GrouperDaoImpl;
import org.ccci.idm.grouper.obj.GrouperGroup;
import org.ccci.idm.obj.SsoUser;
import org.ccci.idm.oid.OidLdapPasswordPolicyService;

import edu.internet2.middleware.grouper.changeLog.provisioning.ChangeEvent;
import edu.internet2.middleware.grouper.changeLog.provisioning.EventProvisioningConnector;
import edu.internet2.middleware.grouper.util.ConfigItem;

/**
 * This connector manages password policies in OID based on group membership.  Users who are in a particular
 * group will have a password policy assigned to them.  Users not in that group will have the default
 * policy.
 * 
 * @author Nathan.Kopp
 *
 */
public class OidPasswordPolicyConnector implements EventProvisioningConnector
{
	@ConfigItem
	private String username = "cn=orcladmin,cn=Users,dc=mygcx,dc=org";
	@ConfigItem
	private String password = "----";
	@ConfigItem
	private String url = "ldap://hart-a933.ccci.org:389";  // URL of OID for "test" environment
    @ConfigItem
    private String grouperFolder = "ccci:itroles:uscore:idm:oid_password_policies:computed";
    @ConfigItem
    private String policyBaseDn = "cn=Common,cn=Products,cn=OracleContext,dc=mygcx,dc=org";
    @ConfigItem
    private String userBaseDn = "cn=sso,dc=mygcx,dc=org";
    
    private GrouperDao dao;

    public OidPasswordPolicyConnector()
	{
		super();
	}

	public boolean dispatchEvent(ChangeEvent event) throws Exception
	{
	    if (dao == null)
        {
            dao = new GrouperDaoImpl(null);
        }

        if (event.getGroupName().startsWith(grouperFolder))
		{
	        OidLdapPasswordPolicyService svc = new OidLdapPasswordPolicyService(username, password, url);
	        
			SsoUser user = dao.loadSsoUser(event.getSubjectId());

			if (user == null)
				throw new Exception("Could not load sso user " + event.getSubjectId());

			GrouperGroup group = dao.loadGroup(event.getGroupName());
			
			if(group==null) return false;

            String userDn = "cn="+user.getSsoGuid()+","+userBaseDn;
            String policyDn = "cn="+group.getDisplayName()+","+policyBaseDn;

			if (event.getEventType().equals(ChangeEvent.ChangeEventType.MEMBERSHIP_ADD.name()))
			{
	            svc.addPasswordPolicy(userDn, policyDn);
				return true;
			}
			else if (event.getEventType().equals(ChangeEvent.ChangeEventType.MEMBERSHIP_DELETE.name()))
			{
			    svc.removePasswordPolicy(userDn, policyDn);
				return true;
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
