package org.ccci.idm.grouperldappc;

import java.util.List;

import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.ccci.idm.grouper.dao.GrouperDao;
import org.ccci.idm.grouper.dao.GrouperDaoImpl;
import org.ccci.idm.grouper.obj.GrouperFolder;
import org.ccci.idm.grouper.obj.GrouperGroup;
import org.ccci.idm.grouperldappc.obj.ExternGroup;
import org.ccci.idm.grouperldappc.obj.ExternUser;
import org.ccci.idm.grouperldappc.obj.GroupForSync;
import org.ccci.idm.grouperldappc.obj.MembershipDifference;
import org.ccci.idm.obj.SsoUser;

import edu.internet2.middleware.grouper.util.ConfigItem;
import edu.internet2.middleware.grouper.util.ConfigUtil;
import edu.internet2.middleware.grouper.util.GrouperUtil;


/**
 * @author Nathan.Kopp
 *
 */
public class BasicDeltaReportTask extends ReportTask
{
    private static final Log LOG = GrouperUtil.getLog(BasicDeltaReportTask.class);
    
    @ConfigItem
    private String grouperRoot = "ccci:itroles:uscore:ldap";
    @ConfigItem
    private String flatteningPathSeparatorCharacter = "-";
    @ConfigItem
    private String computeFromDescr = "false";
    @ConfigItem
    private String flatten = "false";
    @ConfigItem
    protected String systemConnectorClass = "org.ccci.idm.grouperldappc.LdapConnector";
    
    private GrouperDao dao;
    private ExternalSystemConnector connector;
    
    public BasicDeltaReportTask(String customJobName)
    {
        this.customJobName = customJobName;
    }
    
    protected void openConnection() throws Exception
    {
        System.out.println("RecursiveToFlatDeltaReportTask opening connection");
        createDaoIfNecessary();

        if(connector==null)
        {
            Class<?> theClass = GrouperUtil.forName(systemConnectorClass);
            if (LOG.isDebugEnabled())
            {
                LOG.debug("Creating instance of class " + theClass.getCanonicalName());
            }
            connector = (ExternalSystemConnector)GrouperUtil.newInstance(theClass);
            ConfigUtil.readGrouperLoaderConfig(connector, "customJob." + customJobName + ".");
            connector.init();
        }
    }
    
    private void createDaoIfNecessary()
    {
        if (dao == null)
        {
            dao = new GrouperDaoImpl(null);
        }
    }
    
    protected void closeConnection()
    {
        try
        {
            System.out.println("RecursiveToFlatDeltaReportTask closing connection");
            if(connector!=null) connector.close();
            connector = null;
        }
        catch(Exception e)
        {
            // do nothing;
        }
    }

    @Override
    protected void runReport() throws Exception
    {
        DeltaReport report = new DeltaReport();
        GrouperFolder root = dao.loadFolder(grouperRoot);
        if(root!=null)
        {
            //System.out.println("loading grouper info...");
            dao.loadChildGroupsAndFoldersRecursively(root);
            //System.out.println("loaded");
            
            GrouperFolder parent = root;
            reportFolder(root, parent, null, report);
            reportExtraLdapGroups(report, null);
        }
        else
        {
            GrouperGroup group = dao.loadGroup(grouperRoot);
            if(group==null) throw new RuntimeException("could not find "+grouperRoot);
            reportGroup(root, group, null, report);
        }
        
        if(!report.isEmpty())
        {
            String reportStr = generateReport(report);
            sendReport(reportStr);
        }
    }

    private String generateReport(DeltaReport report)
    {
        StringBuffer sb = new StringBuffer();
        sb.append("\n");
        sb.append("******************************************************************\n");
        sb.append(reportName+" for "+systemId+"\n");
        sb.append("******************************************************************\n");
        sb.append("\n\n");
        if(report.getMissingLdapGroups().size()>0)
        {
            sb.append("******************************************************************\n");
            sb.append("Groups in Grouper / NOT in "+systemId+"\n\n");
            sb.append("******************************************************************\n");
            for(GroupForSync missingGroup : report.getMissingLdapGroups())
            {
                sb.append(missingGroup.getFullName()+"\n");
            }
            sb.append("\n");
            sb.append("------------------------------------------------------------------\n");
            sb.append("Script to add groups to "+systemId+"\n");
            sb.append("------------------------------------------------------------------\n");
            sb.append(connector.generateCreationScript(report.getMissingLdapGroups()));
            sb.append("\n");
        }
        if(report.getExtraLdapGroups().size()>0)
        {
            sb.append("******************************************************************\n");
            sb.append("Groups not in Grouper / In "+systemId+"\n");
            sb.append("******************************************************************\n");
            for(GroupForSync extraGroup : report.getExtraLdapGroups())
            {
                sb.append(extraGroup.getFullName()+"\n");
            }
            sb.append("\n");
            sb.append("------------------------------------------------------------------\n");
            sb.append("Script to remove groups from "+systemId+"\n");
            sb.append("------------------------------------------------------------------\n");
            sb.append(connector.generateDeletionScript(report.getExtraLdapGroups()));
            
            sb.append("\n");
        }
        if(report.getMissingLdapMembers().size()>0)
        {
            sb.append("******************************************************************\n");
            sb.append("Members in Grouper / Not in "+systemId+"\n");
            sb.append("******************************************************************\n");
            for(MembershipDifference missingUser : report.getMissingLdapMembers())
            {
                sb.append("missing "+missingUser.getSsoUser().getUsername()+" in group "+missingUser.getExternGroup().getFullPath()+"\n");
            }
            sb.append("\n");
            sb.append("------------------------------------------------------------------");
            sb.append("Script to add members to "+systemId+"\n");
            sb.append("------------------------------------------------------------------");
            sb.append(connector.generateAddMembersScript(report.getMissingLdapMembers()));
            
            sb.append("\n");
            sb.append("------------------------------------------------------------------");
            sb.append("If you want to remove the members from Grouper...\n");
            sb.append("------------------------------------------------------------------");
            String groupId = null;
            for(MembershipDifference missingUser : report.getMissingLdapMembers())
            {
                if(!missingUser.getGrouperGroup().getFullPath().equals(groupId))
                {
                    groupId = missingUser.getGrouperGroup().getFullPath();
                    sb.append("\n");
                    sb.append("Find this group in Grouper: "+groupId+"\n");
                    sb.append("and remove these users:\n");
                }
                sb.append(missingUser.getSsoUser().getUsername()+"\n");
            }
            sb.append("\n");
        }
        if(report.getExtraLdapMembers().size()>0)
        {
            sb.append("******************************************************************\n");
            sb.append("Members NOT in Grouper / In "+systemId+"\n");
            sb.append("******************************************************************\n");
            String groupRdn = null;
            for(MembershipDifference extraUser : report.getExtraLdapMembers())
            {
                if(!extraUser.getExternGroup().getFullPath().equals(groupRdn))
                {
                    groupRdn = extraUser.getExternGroup().getFullPath();
                    sb.append("\nin group "+groupRdn+"\n");
                }
                sb.append("  "+extraUser.getSsoUser().getUsername()+"\n");
            }
            sb.append("\n");
            sb.append("------------------------------------------------------------------\n");
            sb.append("Script to remove members from "+systemId+"\n");
            sb.append("------------------------------------------------------------------\n");
            sb.append(connector.generateRemoveMembersScript(report.getExtraLdapMembers()));
            
            sb.append("\n");
            sb.append("------------------------------------------------------------------\n");
            sb.append("If you want to add the members to Grouper...\n");
            sb.append("------------------------------------------------------------------\n");
            String groupId = null;
            for(MembershipDifference extraUser : report.getExtraLdapMembers())
            {
                if(!extraUser.getGrouperGroup().getFullPath().equals(groupId))
                {
                    groupId = extraUser.getGrouperGroup().getFullPath();
                    sb.append("\n");
                    sb.append("Find this group in Grouper: "+groupId+"\n");
                    sb.append("and import these users:\n");
                }
                sb.append(extraUser.getSsoUser().getUsername()+"\n");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private void reportExtraLdapGroups(DeltaReport report, String relativePath) throws NamingException
    {
        List<ExternGroup> externGroups = connector.loadGroups(relativePath);
        for(ExternGroup externGroup : externGroups)
        {
            String ldapName = externGroup.getFullPath().toLowerCase();
            if(!report.getMatchedLdapGroups().contains(ldapName))
            {
                GroupForSync groupForSync = new GroupForSync(externGroup.getShortName(),relativePath);
                report.getExtraLdapGroups().add(groupForSync);
            }
        }
        if(!isTrue(flatten))
        {
            List<ExternGroup> externContainers = connector.loadFolders(relativePath);
            for(ExternGroup externContainer : externContainers)
            {
                if(relativePath==null) reportExtraLdapGroups(report, externContainer.getShortName());
                else reportExtraLdapGroups(report, relativePath+":"+externContainer.getShortName());
            }
        }
    }

    
    private void reportFolder(GrouperFolder root, GrouperFolder parent, String relativePath, DeltaReport report) throws NamingException
    {
        //System.out.println("working on folder: "+root.getFullDisplayName());
        for(GrouperGroup group : parent.getChildGroups())
        {
            //System.out.println("working on group: "+group.getFullDisplayName());
//            String ldapName = computeGroupLdapName(group, root);
//            String thisGroupBaseDn = computeThisGroupBaseDn(group, root, groupBaseDn);
        
            reportGroup(root, group, relativePath, report);
        }
        for(GrouperFolder folder : parent.getChildFolders())
        {
            if(relativePath==null) reportFolder(root, folder, (isTrue(computeFromDescr)?folder.getDisplayName():folder.getId()), report);
            else reportFolder(root, folder, relativePath+":"+(isTrue(computeFromDescr)?folder.getDisplayName():folder.getId()), report);
        }
    }

    private void reportGroup(GrouperFolder root, GrouperGroup group, String relativePath, DeltaReport report)
    {
        GroupForSync groupForSync = null;
        if(isTrue(flatten) && relativePath!=null)
        {
            groupForSync = new GroupForSync(flattenRelativePath(relativePath)+flatteningPathSeparatorCharacter+(isTrue(computeFromDescr)?group.getDisplayName():group.getId()),null);
        }
        else
        {
            groupForSync = new GroupForSync(isTrue(computeFromDescr)?group.getDisplayName():group.getId(),relativePath);
        }
        
        //GroupForSync gfs2 = computeGroupForSync(group, root);
        
        ExternGroup externGroup = connector.loadGroup(groupForSync);
        
        if(externGroup==null)
        {
            report.getMissingLdapGroups().add(groupForSync);
        }
        else
        {
            report.getMatchedLdapGroups().add(externGroup.getFullPath().toLowerCase());
            List<SsoUser> grouperUsers = dao.getMembers(group.getFullPath());
            reportMembershipDifferences(externGroup, group, externGroup.getMembers(), grouperUsers, report);
        }
    }

    private String flattenRelativePath(String relativePath)
    {
        return relativePath.replace(":", flatteningPathSeparatorCharacter);
    }

    private void reportMembershipDifferences(ExternGroup externGroup, GrouperGroup group, List<ExternUser> ldapUsers, List<SsoUser> grouperUsers, DeltaReport report)
    {
        for(SsoUser grouperUser : grouperUsers)
        {
            //System.out.println("working on grouper user: "+grouperUser);
            boolean found =false;
            for(ExternUser ldapUser : ldapUsers)
            {
                ldapUser.getId();
                ldapUser.getId().toString();
                grouperUser.getSsoGuid();
                if (ldapUser.getId().equalsIgnoreCase(grouperUser.getSsoGuid())) found = true;
            }
            
            if(!found)
            {
                ExternUser externUser = connector.computeExternUserFor(grouperUser);
                report.getMissingLdapMembers().add(new MembershipDifference(externGroup, group, externUser, grouperUser));
            }
        }
        for(ExternUser ldapUser : ldapUsers)
        {
            //System.out.println("working on ldap user: "+ldapUser);
            boolean found =false;
            for(SsoUser grouperUser : grouperUsers) if (ldapUser.getId().equalsIgnoreCase(grouperUser.getSsoGuid())) found = true;

            if(!found)
            {
                SsoUser grouperUser = dao.loadSsoUser(ldapUser.getId());
                if(grouperUser==null)
                {
                    // apparently unresolveable subject
                    continue;
                }
                report.getExtraLdapMembers().add(new MembershipDifference(externGroup, group, ldapUser, grouperUser));
            }
        }
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

    public ExternalSystemConnector getConnector()
    {
        return connector;
    }

    public void setConnector(ExternalSystemConnector connector)
    {
        this.connector = connector;
    }

    public String getGrouperRoot()
    {
        return grouperRoot;
    }

    public void setGrouperRoot(String grouperPrefix)
    {
        this.grouperRoot = grouperPrefix;
    }

    public String getFlatteningPathSeparatorCharacter()
    {
        return flatteningPathSeparatorCharacter;
    }

    public void setFlatteningPathSeparatorCharacter(String flatteningPathSeparatorCharacter)
    {
        this.flatteningPathSeparatorCharacter = flatteningPathSeparatorCharacter;
    }

    public String getFlatten()
    {
        return flatten;
    }

    public void setFlatten(String flatten)
    {
        this.flatten = flatten;
    }

    public String getSystemConnectorClass()
    {
        return systemConnectorClass;
    }

    public void setSystemConnectorClass(String externalConnectorClass)
    {
        this.systemConnectorClass = externalConnectorClass;
    }

    public String getComputeFromDescr()
    {
        return computeFromDescr;
    }

    public void setComputeFromDescr(String computeFromDescr)
    {
        this.computeFromDescr = computeFromDescr;
    }

}
