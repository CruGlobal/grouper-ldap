package org.ccci.idm.grouperldappc;

import java.util.ArrayList;
import java.util.List;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.SearchResult;

import org.apache.commons.logging.Log;
import org.ccci.idm.grouper.dao.GrouperDao;
import org.ccci.idm.grouper.dao.GrouperDaoImpl;
import org.ccci.idm.grouper.obj.GrouperFolder;
import org.ccci.idm.grouper.obj.GrouperGroup;
import org.ccci.idm.ldap.Ldap;
import org.ccci.idm.obj.SsoUser;

import edu.internet2.middleware.grouper.util.ConfigItem;
import edu.internet2.middleware.grouper.util.GrouperUtil;


/**
 * @author Nathan.Kopp
 *
 */
public class RecursiveToFlatDeltaReportTask extends DeltaReportTask
{
    private static final Log LOG = GrouperUtil.getLog(RecursiveToFlatDeltaReportTask.class);
    
    @ConfigItem
    private String userRdnAttribName = "cn";
    @ConfigItem
    private String userBaseDn = "cn=sso,dc=mygcx,dc=org";
    @ConfigItem
    private String groupRdnAttrib = "cn";
    @ConfigItem
    private String groupBaseDn = "ou=Accounts,ou=Stellent,ou=Xellerate Users,cn=sso,dc=mygcx,dc=org";
    @ConfigItem
    private String groupLdapClass = "groupOfUniqueNames";
    @ConfigItem
    private String grouperPrefix = "ccci:itroles:uscore:stellent:accounts";
    @ConfigItem
    private String ldapUsername = "cn=B3712AFF-88C6-A4B8-6F50-A2E4C4C6A241,cn=sso,dc=mygcx,dc=org";
    @ConfigItem
    private String ldapPassword = "Grouper1";
    @ConfigItem
    private String ldapUrl = "ldap://hart-a933.ccci.org:389";
    @ConfigItem
    private String flatteningPathSeparatorCharacter = "-";
    @ConfigItem
    private String computeFromDescr = "true";
    
    private GrouperDao dao;
    private Ldap ldap;
    
    protected void openConnection() throws Exception
    {
        System.out.println("RecursiveToFlatDeltaReportTask opening connection");
        createDaoIfNecessary();
        ldap = new Ldap(ldapUrl,ldapUsername,ldapPassword);
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
            if(ldap!=null) ldap.close();
        }
        catch(Exception e)
        {
            // do nothing;
        }
    }

    @Override
    protected void runReport() throws Exception
    {
        GrouperFolder root = dao.loadFolder(grouperPrefix);
        dao.loadChildGroupsAndFoldersRecursively(root);
        
        List<String> matchedGroups = new ArrayList<String>();
        GrouperFolder parent = root;
        reportFolder(root, parent, matchedGroups);
        reportExtraLdapGroups(matchedGroups);
    }

    private void reportExtraLdapGroups(List<String> matchedGroups) throws NamingException
    {
        List<SearchResult> ldapGroups = ldap.search2(groupBaseDn, "(objectClass="+groupLdapClass+")", new String[]{groupRdnAttrib});
        for(SearchResult ldapGroup : ldapGroups)
        {
            String ldapName = ldapGroup.getAttributes().get(groupRdnAttrib).get().toString();
            if(!matchedGroups.contains(ldapName))
            {
                addReportLine("Group is missing in Grouper: "+ldapName);
            }
        }
    }

    private void reportFolder(GrouperFolder root, GrouperFolder parent, List<String> matchedGroups) throws NamingException
    {
        
        for(GrouperGroup group : parent.getChildGroups())
        {
            String ldapName = computeGroupLdapName(group, root);
            List<SearchResult> ldapGroupMatches = ldap.search2(groupBaseDn, "("+groupRdnAttrib+"="+ldapName+")", new String[]{"uniqueMember"});
            if (ldapGroupMatches.size()>1)
            {
                throw new RuntimeException("matched more than one LDAP group for "+ldapName);
            }
            else if(ldapGroupMatches.size()==0)
            {
                addReportLine("Group is missing in LDAP: "+ldapName);
            }
            else
            {
                matchedGroups.add(ldapName);
                
                List<String> ldapUsers = gatherLdapGroupMembershipAsLdapDns(ldapGroupMatches);
                List<String> grouperUsers = gatherGrouperGroupMembershipAsLdapDns(group);
                reportMembershipDifferences(ldapName, ldapUsers, grouperUsers);
            }
        }
        for(GrouperFolder folder : parent.getChildFolders())
        {
            reportFolder(root, folder, matchedGroups);
        }
    }

    private void reportMembershipDifferences(String ldapName, List<String> ldapUsers, List<String> grouperUsers)
    {
        for(String grouperUser : grouperUsers)
        {
            if(!ldapUsers.contains(grouperUser))
                addReportLine("User membership found in Grouper but not LDAP: "+grouperUser+", "+ldapName);
        }
        for(String ldapUser : ldapUsers)
        {
            if(!grouperUsers.contains(ldapUser))
                addReportLine("User membership found in LDAP but not Grouper: "+ldapUser+", "+ldapName);
        }
    }

    private List<String> gatherGrouperGroupMembershipAsLdapDns(GrouperGroup group)
    {
        List<String> grouperUsers = new ArrayList<String>();
        for(SsoUser user : dao.getMembers(group.getFullPath()))
        {
            String userDn = userRdnAttribName+"="+user.getSsoGuid()+","+userBaseDn;
            grouperUsers.add(userDn.toLowerCase());
        }
        return grouperUsers;
    }

    private List<String> gatherLdapGroupMembershipAsLdapDns(List<SearchResult> ldapGroupMatches) throws NamingException
    {
        List<String> ldapUsers = new ArrayList<String>();
        if(ldapGroupMatches.get(0).getAttributes().get("uniqueMember")==null) return ldapUsers;
        NamingEnumeration<?> enum1 = ldapGroupMatches.get(0).getAttributes().get("uniqueMember").getAll();
        while(enum1.hasMore())
        {
            ldapUsers.add(enum1.next().toString().toLowerCase());
        }
        return ldapUsers;
    }
    
    private void addReportLine(String string)
    {
        System.out.println(string);
    }

    private String computeGroupLdapName(GrouperGroup group, GrouperFolder baseFolder)
    {
        if("y".equalsIgnoreCase(computeFromDescr) || "true".equalsIgnoreCase(computeFromDescr))
        {
            int start = baseFolder.getFullDisplayName().length()+1;
            String name = group.getFullDisplayName().substring(start, group.getFullDisplayName().length());
            name = name.replace(":", flatteningPathSeparatorCharacter);
            return name;
        }
        else
        {
            int start = grouperPrefix.length()+1;
            String name = group.getFullPath().substring(start, group.getFullPath().length());
            name = name.replace(":", flatteningPathSeparatorCharacter);
            return name;
        }
    }


}
