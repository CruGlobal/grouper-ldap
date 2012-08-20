package org.ccci.idm.grouperldappc;

import org.apache.commons.logging.Log;
import org.ccci.idm.grouper.dao.GrouperDao;
import org.ccci.idm.grouper.dao.GrouperDaoImpl;
import org.ccci.idm.grouper.obj.GrouperFolder;
import org.ccci.idm.grouper.obj.GrouperGroup;
import org.ccci.idm.grouperldappc.obj.GroupForSync;
import org.ccci.idm.obj.SsoUser;

import edu.internet2.middleware.grouper.changeLog.provisioning.ChangeEvent;
import edu.internet2.middleware.grouper.changeLog.provisioning.EventProvisioningConnector;
import edu.internet2.middleware.grouper.util.ConfigItem;
import edu.internet2.middleware.grouper.util.ConfigUtil;
import edu.internet2.middleware.grouper.util.GrouperUtil;

/**
 * This connector provides a one-way sync from one stem in grouper to one base DN in LDAP.
 * It will create and delete groups in LDAP to correspond to groups in the specified stem,
 * and will manage membership in LDAP to correspond to Grouper membership.
 * 
 * @author Nathan.Kopp
 *
 */
public class BasicEventConnector implements EventProvisioningConnector
{
    private static final Log LOG = GrouperUtil.getLog(BasicEventConnector.class);
    
    protected String consumerName;
    
    @ConfigItem
    protected String grouperRoot = "ccci:itroles:uscore:ldap";
    @ConfigItem
    protected String flatteningPathSeparatorCharacter = "-";
    @ConfigItem
    protected String computeFromDescr = "false";
    @ConfigItem
    protected String flatten = "false";
    @ConfigItem
    protected String systemConnectorClass = "org.ccci.idm.grouperldappc.LdapConnector";

    protected GrouperDao dao;


	public BasicEventConnector()
	{
		super();
		LOG.debug("Constructed NonRecursiveLdapGroupConnector");
	}

	public boolean dispatchEvent(ChangeEvent event) throws Exception
	{
		String groupName = null;
		if(isMembershipEvent(event)) groupName = event.getGroupName();
		else groupName = event.getName();

		if (groupName!=null && groupName.startsWith(grouperRoot))
		{
		    LOG.debug("group: "+groupName+" MATCHED PREFIX");
	        createDaoIfNecessary();
	        
	        Class<?> theClass = GrouperUtil.forName(systemConnectorClass);
            if (LOG.isDebugEnabled())
            {
                LOG.debug("Creating instance of class " + theClass.getCanonicalName());
            }
            ExternalSystemConnector connector = (ExternalSystemConnector)GrouperUtil.newInstance(theClass);
	        ConfigUtil.readGrouperLoaderConfig(connector, "changeLog.consumer." + consumerName + ".connector.");
	        connector.init();
            
            try
            {
                GrouperGroup group = dao.loadGroup(groupName);
                if(group==null)
                {
                    LOG.info("could not load group: "+groupName);
                    return false;
                }
                GroupForSync groupForSync = null;
                
                GrouperFolder baseFolder = dao.loadFolder(grouperRoot);
                if(baseFolder!=null)
                {
                    groupForSync = computeGroupForSync(group, baseFolder);
                }
                else
                {
                    GrouperGroup baseGroup = dao.loadGroup(grouperRoot);
                    if(baseGroup==null) throw new RuntimeException("cannot find "+grouperRoot);
                    //groupForSync = new GroupForSync(grouperPrefix.substring(grouperPrefix.lastIndexOf(":")+1), grouperPrefix.substring(0,grouperPrefix.lastIndexOf(":")));
                    groupForSync = new GroupForSync(grouperRoot.substring(grouperRoot.lastIndexOf(":")+1), null);
                }
                
    		    if(isMembershipEvent(event))
    		    {
        			SsoUser user = dao.loadSsoUser(event.getSubjectId());
        			if (user == null)
        			{
        			    LOG.info("could not load user with subjectId: "+event.getSubjectId());
        			    return false;
        			}
        			
        			
        			try
        			{
            			if (event.getEventType().equals(ChangeEvent.ChangeEventType.MEMBERSHIP_ADD.name()))
            			{
            			    connector.addUserToGroup(groupForSync, user);
            			}
            			else if (event.getEventType().equals(ChangeEvent.ChangeEventType.MEMBERSHIP_DELETE.name()))
            			{
            			    connector.removeUserFromGroup(groupForSync, user);
            			}
        			}
        			catch(javax.naming.directory.AttributeInUseException e)
        			{
    			        LOG.info(e.getMessage());
        			}
        			return true;
    		    }
    		    else if(event.getEventType().equals(ChangeEvent.ChangeEventType.GROUP_ADD.name()))
                {
    		        return connector.createGroup(groupForSync);
                }
    		    else if(event.getEventType().equals(ChangeEvent.ChangeEventType.GROUP_DELETE.name()))
    		    {
    		        return connector.removeGroup(groupForSync);
    		    }
            }
            finally
            {
                connector.close();
            }
		}

		return false;
	}
	
	private boolean isTrue(String s)
	{
	    return "true".equalsIgnoreCase(s) || "yes".equalsIgnoreCase(s) || "y".equalsIgnoreCase(s);
	}
	
	private GroupForSync computeGroupForSync(GrouperGroup group, GrouperFolder baseFolder)
    {
	    if(isTrue(flatten))
        {
            String groupLdapName = getGroupNameRelativeToBase(group, baseFolder);
            groupLdapName = groupLdapName.replace(":", flatteningPathSeparatorCharacter);
            return new GroupForSync(groupLdapName, null);
        }
        else
        {
            String relativePathStr = getGroupNameRelativeToBase(group, baseFolder);
            int idx = relativePathStr.lastIndexOf(":");
            if(idx>-1)
            {
                return new GroupForSync(relativePathStr.substring(idx+1), relativePathStr.substring(0,idx));
            }
            else
            {
                return new GroupForSync(relativePathStr, null);
            }
        }
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
            int start = grouperRoot.length()+1;
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
	    this.consumerName = consumerName;
	    if(grouperRoot.endsWith(":")) grouperRoot = grouperRoot+":";
	}
}
