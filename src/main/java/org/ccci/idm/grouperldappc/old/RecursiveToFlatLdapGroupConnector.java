package org.ccci.idm.grouperldappc.old;

import java.util.List;

import javax.naming.NamingException;
import javax.naming.directory.SearchResult;

import org.apache.commons.logging.Log;
import org.ccci.idm.grouper.dao.GrouperDao;
import org.ccci.idm.grouper.dao.GrouperDaoImpl;
import org.ccci.idm.grouper.obj.GrouperFolder;
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
public class RecursiveToFlatLdapGroupConnector implements EventProvisioningConnector
{
    private static final Log LOG = GrouperUtil.getLog(RecursiveToFlatLdapGroupConnector.class);
    
    @ConfigItem
    private String userRdnAttribName = "cn";
    @ConfigItem
    private String userBaseDn = "CN=Users,CN=idm,DC=cru,DC=org";
    @ConfigItem
    private String groupRdnAttrib = "CN";
    @ConfigItem
    private String groupBaseDn = "CN=Other,CN=Groups,CN=idm,DC=cru,DC=org";
    @ConfigItem
    private String groupLdapClasses = "group";
    @ConfigItem
    private String grouperPrefix = "ccci:itroles:uscore:ldap";
    @ConfigItem
	private String ldapUsername = "GrouperAdmin";
	@ConfigItem
	private String ldapPassword = "sdkfbaw34(*Pkjy32";
	@ConfigItem
	private String ldapUrl = "ldaps://twidma27.net.ccci.org:636";
    @ConfigItem
    private String flatteningPathSeparatorCharacter = "-";
    @ConfigItem
    private String computeFromDescr = "false";
    
    @ConfigItem
    private String containerLdapClasses = "container";
    @ConfigItem
    private String containerRdnAttrib = "CN";
    @ConfigItem
    private String flatten = "false";

    private GrouperDao dao;


	public RecursiveToFlatLdapGroupConnector()
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
                
                GrouperFolder baseFolder = dao.loadFolder(grouperPrefix);
                
                
                String groupDn = computeGroupLdapDn(group, baseFolder);
                
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
    		            createGroupInLdap(ldap, group, groupDn, baseFolder);
    		        }
    		        catch(javax.naming.NameAlreadyBoundException e)
    		        {
    		            LOG.error(groupDn+"already exists in"+ldapUrl);
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
	
	private boolean isTrue(String s)
	{
	    return "true".equalsIgnoreCase(s) || "yes".equalsIgnoreCase(s) || "y".equalsIgnoreCase(s);
	}

	private String computeGroupLdapDn(GrouperGroup group, GrouperFolder baseFolder)
    {
	    if(isTrue(flatten))
	    {
            String groupLdapName = getGroupNameRelativeToBase(group, baseFolder);
            groupLdapName = groupLdapName.replace(":", flatteningPathSeparatorCharacter);
            return groupRdnAttrib+"="+groupLdapName+","+groupBaseDn;
	    }
	    else
	    {
	        String groupLdapName = getGroupNameRelativeToBase(group, baseFolder);
	        String[] groupNames = groupLdapName.split(":");
	        String dn = groupBaseDn;
	        for(int i=0; i<groupNames.length; i++)
	        {
	            String name = groupNames[i];
	            dn = ((i==groupNames.length-1)?groupRdnAttrib:containerRdnAttrib)+"="+name+","+dn;
	        }
	        return dn;
	    }
    }
	
	
    private String computeFlatGroupLdapName(GrouperGroup group, GrouperFolder baseFolder)
    {
        String name = getGroupNameRelativeToBase(group, baseFolder);
        name = name.replace(":", flatteningPathSeparatorCharacter);
        return name;
    }

    private String getGroupNameRelativeToBase(GrouperGroup group, GrouperFolder baseFolder)
    {
        String name = null;
        if(isTrue(computeFromDescr))
        {
            int start = baseFolder.getFullDisplayName().length()+1;
            name = group.getFullDisplayName().substring(start, group.getFullDisplayName().length());
        }
        else
        {
            int start = grouperPrefix.length()+1;
            name = group.getFullPath().substring(start, group.getFullPath().length());
        }
        return name;
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

    private void createGroupInLdap(Ldap ldap, GrouperGroup group, String groupDn, GrouperFolder baseFolder) throws NamingException
    {
        if(isTrue(flatten))
        {
            String groupLdapName = computeFlatGroupLdapName(group, baseFolder);
            String[] attribNames = new String[] { groupRdnAttrib };
            String[] attribValues = new String[] { groupLdapName };
            String[] ldapClasses = groupLdapClasses.split("\\s*,\\s*");
            ldap.createEntity(groupDn, attribNames, attribValues, ldapClasses);
        }
        else
        {
            String[] groupClasses = groupLdapClasses.split("\\s*,\\s*");
            String[] containerClasses = containerLdapClasses.split("\\s*,\\s*");
            String groupLdapName = getGroupNameRelativeToBase(group, baseFolder);
            String[] groupNames = groupLdapName.split(":");
            String dn = groupBaseDn;
            for(int i=0; i<groupNames.length; i++)
            {
                String name = groupNames[i];
                
                if(i==groupNames.length-1)
                {
                    // create the group
                    dn = groupRdnAttrib+"="+name+","+dn;
                    String[] attribNames = new String[] { groupRdnAttrib };
                    String[] attribValues = new String[] { name };
                    ldap.createEntity(dn, attribNames, attribValues, groupClasses);
                }
                else
                {
                    // maybe create the folder
                    List<SearchResult> results = ldap.search2(dn, "("+containerRdnAttrib+"="+name+")", null);
                    dn = containerRdnAttrib+"="+name+","+dn;  // this has a side-effect on the loop
                    if(results.size()==0)
                    {
                        String[] attribNames = new String[] { containerRdnAttrib };
                        String[] attribValues = new String[] { name };
                        ldap.createEntity(dn, attribNames, attribValues, containerClasses);
                    }
                }
                
            }
        }
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
