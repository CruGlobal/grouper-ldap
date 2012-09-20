package org.ccci.idm.grouperldappc;

import java.util.ArrayList;
import java.util.List;

import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.SearchResult;

import org.apache.commons.logging.Log;
import org.ccci.idm.grouperldappc.obj.ExternGroup;
import org.ccci.idm.grouperldappc.obj.ExternUser;
import org.ccci.idm.grouperldappc.obj.GroupForSync;
import org.ccci.idm.grouperldappc.obj.MembershipDifference;
import org.ccci.idm.ldap.Ldap;
import org.ccci.idm.obj.SsoUser;
import org.ccci.util.Exceptions;
import org.ccci.util.NkUtil;

import edu.internet2.middleware.grouper.util.ConfigItem;
import edu.internet2.middleware.grouper.util.GrouperUtil;

public class LdapConnector implements ExternalSystemConnector
{
    protected Ldap ldap;
    
    @ConfigItem
    private String userRdnAttribName = "cn";
    @ConfigItem
    private String userBaseDn = "CN=Users,CN=idm,DC=cru,DC=org";
    @ConfigItem
    private String groupRdnAttrib = "CN";
    @ConfigItem
    private String groupBaseDn = "CN=Other,CN=Groups,CN=idm,DC=cru,DC=org";
    @ConfigItem
    private String groupClass = "group";
    @ConfigItem
    private String ldapUsername = "GrouperAdmin";
    @ConfigItem
    private String ldapPassword = "sdkfbaw34(*Pkjy32";
    @ConfigItem
    private String ldapUrl = "ldap://twidma27.net.ccci.org:636";
    @ConfigItem
    private String containerClass = "container";
    @ConfigItem
    private String containerRdnAttrib = "CN";
    @ConfigItem
    private String memberAttribName = "member";

    private static final Log LOG = GrouperUtil.getLog(LdapConnector.class);
    
    public LdapConnector()
    {
    }
    
    public void init()
    {
        try
        {
            ldap = new Ldap(ldapUrl,ldapUsername,ldapPassword);
        }
        catch(Throwable t)
        {
            throw Exceptions.wrapDontThrow(t);
        }
    }
    
    public void close() throws NamingException
    {
        try
        {
            ldap.close();
        }
        catch(Throwable t)
        {
            throw Exceptions.wrapDontThrow(t);
        }
    }

    public void addUserToGroup(GroupForSync group, SsoUser user) throws Exception
    {
        ExternGroup ldapGroup = computeExternGroupInfo(group);
        String userDn = userRdnAttribName+"="+user.getSsoGuid()+","+userBaseDn;
        LOG.debug("adding user to group: "+userDn+" "+ldapGroup.getFullPath());
        ldap.addUserToGroup(ldapGroup.getFullPath(), userDn);
    }

    public void removeUserFromGroup(GroupForSync group, SsoUser user) throws Exception
    {
        ExternGroup ldapGroup = computeExternGroupInfo(group);
        String userDn = userRdnAttribName+"="+user.getSsoGuid()+","+userBaseDn;
        LOG.debug("removing user from group: "+userDn+" "+ldapGroup.getFullPath());
        ldap.removeUserFromGroup(ldapGroup.getFullPath(), userDn);
    }
    
    public boolean createGroup(GroupForSync group)
    {
        ExternGroup ldapGroup = computeExternGroupInfo(group);
        LOG.debug("adding group: "+ldapGroup.getFullPath());
        
        try
        {
            if(!NkUtil.isBlank(group.getRelativePath())) addMissingContainers(group);

            String[] groupClasses = new String[] { groupClass };
            String[] attribNames = new String[] { groupRdnAttrib };
            String[] attribValues = new String[] { ldapGroup.getShortName() };
            
            ldap.createEntity(ldapGroup.getFullPath(), attribNames, attribValues, groupClasses);
            return true;
        }
        catch(javax.naming.NameAlreadyBoundException e)
        {
            LOG.error(ldapGroup.getFullPath()+"already exists in"+ldapUrl);
            return true;
        }
        catch(Exception e)
        {
            if(e.getMessage().contains("already exists"))
            {
                LOG.info(e.getMessage());
                return true;
            }
            else
            {
                LOG.error(e);
                return false;
            }
        }
    }

    public boolean removeGroup(GroupForSync groupForSync)
    {
        ExternGroup ldapGroup = computeExternGroupInfo(groupForSync);
        LOG.debug("removing group: "+ldapGroup.getFullPath());
        
        try
        {
            ldap.deleteEntity(ldapGroup.getFullPath());
        }
        catch(Exception e)
        {
            LOG.error(e);
        }
        return true;
    }

    private void addMissingContainers(GroupForSync group) throws NamingException
    {
        String[] relativePathSplit = group.getRelativePath().split(":");
        String dn = groupBaseDn;
        for(String folderName : relativePathSplit)
        {
            dn = addContainerIfNecessary(dn, folderName);
        }
    }

    private String addContainerIfNecessary(String dn, String folderName) throws NamingException
    {
        List<SearchResult> results = ldap.search2(dn, "("+containerRdnAttrib+"="+folderName+")", null);
        dn = containerRdnAttrib+"="+folderName+","+dn;  // this has a side-effect on the loop
        if(results.size()==0)
        {
            String[] containerClasses = new String[] { containerClass };
            String[] attribNames = new String[] { containerRdnAttrib };
            String[] attribValues = new String[] { folderName };
            ldap.createEntity(dn, attribNames, attribValues, containerClasses);
        }
        return dn;
    }
    
    private ExternGroup computeExternGroupInfo(GroupForSync group)
    {
        String parentDn = computeParentDn(group.getRelativePath());
        return new ExternGroup(group.getShortName(), parentDn, groupRdnAttrib+"="+group.getShortName()+","+parentDn);
    }

    private String computeParentDn(String relativePath)
    {
        String parentDn = groupBaseDn;
        if(!NkUtil.isBlank(relativePath))
        {
            String[] relativePathSplit = relativePath.split(":");
            for(String folderName : relativePathSplit)
            {
                parentDn = containerRdnAttrib+"="+folderName+","+parentDn;
            }
        }
        return parentDn;
    }

    @Override
    public List<ExternGroup> loadGroups(String relativePath)
    {
        try
        {
            String parentDn = computeParentDn(relativePath);
            List<SearchResult> ldapGroups = ldap.search2(parentDn, "(objectClass="+groupClass+")", new String[]{groupRdnAttrib});
            List<ExternGroup> retVal = new ArrayList<ExternGroup>(ldapGroups.size());
            for(SearchResult ldapGroup : ldapGroups)
            {
                String shortName = ldapGroup.getAttributes().get(groupRdnAttrib).get().toString();
                String dn = ldapGroup.getNameInNamespace();
                retVal.add(new ExternGroup(shortName, parentDn, dn));
            }
            return retVal;
        }
        catch(Throwable t)
        {
            throw Exceptions.wrapDontThrow(t);
        }
    }
    
    @Override
    public ExternGroup loadGroup(GroupForSync groupForSync)
    {
        try
        {
            ExternGroup ldapGroup = computeExternGroupInfo(groupForSync);
            
            List<SearchResult> ldapGroupMatches = ldap.search2(ldapGroup.getParentPath(), "("+groupRdnAttrib+"="+ldapGroup.getShortName()+")", new String[]{memberAttribName});
            
            if (ldapGroupMatches.size()>1)
            {
                throw new RuntimeException("matched more than one LDAP group for "+ldapGroup.getFullPath());
            }
            else if(ldapGroupMatches.size()==0)
            {
                return null;
            }
            else
            {
                boolean done = false;
                
                while(!done)
                {
                    Attribute memberAttribute = getMemberAttribute(ldapGroupMatches);
                    
                    if(memberAttribute==null) { done = true; break; }
                    if(memberAttribute.size()==0) { done = true; break; }
                    
                    NamingEnumeration<?> enum1 = memberAttribute.getAll();
                    while(enum1.hasMore())
                    {
                        String userDn = enum1.next().toString().toLowerCase();
                        ldapGroup.addUser(extractIdFromDn(userDn),userDn);
                    }
                    
                    if(isRange(memberAttribute))
                    {
                        int max = getRangeMax(memberAttribute);
                        if(max<0)
                        {
                            done = true;
                        }
                        else
                        {
                            String attribWithRange = memberAttribName+";range="+(max+1)+"-*";
                            ldapGroupMatches = ldap.search2(groupBaseDn, "("+groupRdnAttrib+"="+ldapGroup.getShortName()+")", new String[]{attribWithRange});
                        }
                    }
                    else
                    {
                        done = true;
                    }
                }
                ldapGroup.ensureMemberList();
                return ldapGroup;
            }
        }
        catch(NameNotFoundException e)
        {
            return null;
        }
        catch(Throwable t)
        {
            throw Exceptions.wrapDontThrow(t);
        }
    }
    
    private Attribute getMemberAttribute(List<SearchResult> ldapGroupMatches) throws NamingException
    {
        Attribute memberAttribute = ldapGroupMatches.get(0).getAttributes().get(memberAttribName);
        if(memberAttribute==null || memberAttribute.size()==0)
        {
            NamingEnumeration<?> attribEnum = ldapGroupMatches.get(0).getAttributes().getAll();
            while(attribEnum.hasMore())
            {
                Attribute attrib = (Attribute)attribEnum.next();
                if(attrib.getID().toLowerCase().startsWith(memberAttribName.toLowerCase()) && !attrib.getID().equalsIgnoreCase(memberAttribName)) return attrib;
            }
        }
        return memberAttribute;
    }
    
    private boolean isRange(Attribute attrib) throws NamingException
    {
        return attrib.getID().contains("range");
    }
    
    private int getRangeMax(Attribute attrib) throws NamingException
    {
        String name = attrib.getID();
        String range = name.substring(name.indexOf(";range")+";range".length());
        int idx = range.indexOf('-');
        String endNum = range.substring(idx+1);
        if(endNum.equals("*")) return -1;
        return Integer.parseInt(endNum);
    }

    
    @Override
    public List<ExternGroup> loadFolders(String relativePath)
    {
        try
        {
            String parentDn = computeParentDn(relativePath);
            List<SearchResult> ldapContainers = ldap.search2(parentDn, "(objectClass="+containerClass+")", new String[]{containerRdnAttrib});
            List<ExternGroup> retVal = new ArrayList<ExternGroup>(ldapContainers.size());
            for(SearchResult ldapContainer : ldapContainers)
            {
                if(ldapContainer.getNameInNamespace().equalsIgnoreCase(parentDn)) continue;
                String shortName = ldapContainer.getAttributes().get(containerRdnAttrib).get().toString();
                String dn = ldapContainer.getNameInNamespace();
                retVal.add(new ExternGroup(shortName, parentDn, dn));
            }
            return retVal;
        }
        catch(Throwable t)
        {
            throw Exceptions.wrapDontThrow(t);
        }
    }
    
    private String extractIdFromDn(String dn)
    {
        return dn.substring(3, 39);
    }

    @Override
    public String generateCreationScript(List<GroupForSync> missingLdapGroups)
    {
        StringBuffer sb = new StringBuffer();
        for(GroupForSync missingGroup : missingLdapGroups)
        {
            sb.append("\n");
            sb.append("dn: "+computeExternGroupInfo(missingGroup).getFullPath()+"\n");
            sb.append("changetype: add\n");
            sb.append("objectclass: "+groupClass+"\n");
            sb.append("objectclass: top\n");
            // do we need "cn"????
            //sb.append("cn: PublicAdmin");
        }
        return sb.toString();
    }

    @Override
    public String generateDeletionScript(List<GroupForSync> extraLdapGroups)
    {
        StringBuffer sb = new StringBuffer();
        for(GroupForSync extraGroup : extraLdapGroups)
        {
            sb.append("\n");
            sb.append("dn: "+computeExternGroupInfo(extraGroup).getFullPath()+"\n");
            sb.append("changetype: delete\n");
        }
        return sb.toString();
    }

    @Override
    public String generateAddMembersScript(List<MembershipDifference> missingLdapMembers)
    {
        StringBuffer sb = new StringBuffer();
        String lastFullPath = null;
        for(MembershipDifference missingUser : missingLdapMembers)
        {
            if(!missingUser.getExternGroup().getFullPath().equals(lastFullPath))
            {
                lastFullPath = missingUser.getExternGroup().getFullPath();
                sb.append("\n");
                sb.append("dn: "+lastFullPath+"\n");
                sb.append("changetype: modify\n");
                sb.append("add: "+memberAttribName+"\n");
            }
            sb.append(memberAttribName+": "+missingUser.getExternUser().getFullPath()+"\n");
        }
        return sb.toString();

    }

    @Override
    public String generateRemoveMembersScript(List<MembershipDifference> extraLdapMembers)
    {
        StringBuffer sb = new StringBuffer();
        String lastFullPath = null;
        for(MembershipDifference extraUser : extraLdapMembers)
        {
            if(!extraUser.getExternGroup().getFullPath().equals(lastFullPath))
            {
                lastFullPath = extraUser.getExternGroup().getFullPath();
                sb.append("\n");
                sb.append("dn: "+lastFullPath+"\n");
                sb.append("changetype: modify\n");
                sb.append("delete: "+memberAttribName+"\n");
            }
            sb.append(memberAttribName+": "+extraUser.getExternUser().getFullPath()+"\n");
        }
        return sb.toString();

    }

    @Override
    public ExternUser computeExternUserFor(SsoUser grouperUser)
    {
        return new ExternUser(grouperUser.getSsoGuid(), userRdnAttribName+"="+grouperUser.getSsoGuid()+","+userBaseDn);
    }

    public Ldap getLdap()
    {
        return ldap;
    }

    public void setLdap(Ldap ldap)
    {
        this.ldap = ldap;
    }

    public String getUserRdnAttribName()
    {
        return userRdnAttribName;
    }

    public void setUserRdnAttribName(String userRdnAttribName)
    {
        this.userRdnAttribName = userRdnAttribName;
    }

    public String getUserBaseDn()
    {
        return userBaseDn;
    }

    public void setUserBaseDn(String userBaseDn)
    {
        this.userBaseDn = userBaseDn;
    }

    public String getGroupRdnAttrib()
    {
        return groupRdnAttrib;
    }

    public void setGroupRdnAttrib(String groupRdnAttrib)
    {
        this.groupRdnAttrib = groupRdnAttrib;
    }

    public String getGroupBaseDn()
    {
        return groupBaseDn;
    }

    public void setGroupBaseDn(String groupBaseDn)
    {
        this.groupBaseDn = groupBaseDn;
    }

    public String getGroupClass()
    {
        return groupClass;
    }

    public void setGroupClass(String groupClass)
    {
        this.groupClass = groupClass;
    }

    public String getLdapUsername()
    {
        return ldapUsername;
    }

    public void setLdapUsername(String ldapUsername)
    {
        this.ldapUsername = ldapUsername;
    }

    public String getLdapPassword()
    {
        return ldapPassword;
    }

    public void setLdapPassword(String ldapPassword)
    {
        this.ldapPassword = ldapPassword;
    }

    public String getLdapUrl()
    {
        return ldapUrl;
    }

    public void setLdapUrl(String ldapUrl)
    {
        this.ldapUrl = ldapUrl;
    }

    public String getContainerClass()
    {
        return containerClass;
    }

    public void setContainerClass(String containerClass)
    {
        this.containerClass = containerClass;
    }

    public String getContainerRdnAttrib()
    {
        return containerRdnAttrib;
    }

    public void setContainerRdnAttrib(String containerRdnAttrib)
    {
        this.containerRdnAttrib = containerRdnAttrib;
    }

    public String getMemberAttribName()
    {
        return memberAttribName;
    }

    public void setMemberAttribName(String memberAttribName)
    {
        this.memberAttribName = memberAttribName;
    }

}
