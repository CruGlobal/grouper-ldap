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
import org.ccci.util.mail.EmailAddress;
import org.ccci.util.mail.MailMessage;
import org.ccci.util.mail.MailMessageFactory;

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
    //private String groupBaseDn = "ou=Accounts,ou=Stellent,ou=Xellerate Users,cn=sso,dc=mygcx,dc=org";
    private String groupBaseDn = "ou=Roles,ou=Stellent,ou=Xellerate Users,cn=sso,dc=mygcx,dc=org";
    @ConfigItem
    private String groupLdapClass = "groupOfUniqueNames";
    @ConfigItem
    //private String grouperPrefix = "ccci:itroles:uscore:stellent:accounts";
    private String grouperPrefix = "ccci:itroles:uscore:stellent:roles";
    @ConfigItem
    private String ldapUsername = "cn=B3712AFF-88C6-A4B8-6F50-A2E4C4C6A241,cn=sso,dc=mygcx,dc=org";
    @ConfigItem
    private String ldapPassword = "Grouper1";
    @ConfigItem
    private String ldapUrl = "ldap://oidtst.ccci.org:389";
    @ConfigItem
    private String flatteningPathSeparatorCharacter = "-";
    @ConfigItem
    private String computeFromDescr = "true";
    @ConfigItem
    private String smtpHost = "smtp1.ccci.org";
    @ConfigItem
    private String reportRecipients = "nathan.kopp@ccci.org";
    @ConfigItem
    private String reportSender = "itm-test@ccci.org";
    @ConfigItem
    private String systemId = "IdM Test";
    @ConfigItem
    private String reportName = "Stellent Grouper-LDAP Comparison Report";
    
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
        
        DeltaReport report = new DeltaReport();
        
        GrouperFolder parent = root;
        reportFolder(root, parent, report);
        reportExtraLdapGroups(report);
        
        String reportStr = generateReport(report);
        
        System.out.println(reportStr);
        
        sendReport(reportStr);
    }

    private void sendReport(String reportStr) throws Exception
    {
        MailMessage mailMessage = new MailMessageFactory(smtpHost).createApplicationMessage();

        String to[] = reportRecipients.split(",");
        for (String address : to)
            mailMessage.addTo(EmailAddress.valueOf(address), "");

        mailMessage.setFrom(EmailAddress.valueOf(reportSender), systemId);

        mailMessage.setMessage(reportName+" for "+systemId, reportStr, false);
        
        mailMessage.sendToAll();
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
            sb.append("Groups missing from LDAP that need to be added\n\n");
            sb.append("******************************************************************\n");
            for(String missingGroup : report.getMissingLdapGroups())
            {
                sb.append(missingGroup+"\n");
            }
            sb.append("\n");
            sb.append("------------------------------------------------------------------\n");
            sb.append("LDIF to add missing groups into LDAP\n");
            sb.append("------------------------------------------------------------------\n");
            for(String missingGroup : report.getMissingLdapGroups())
            {
                sb.append("\n");
                sb.append("dn: "+groupRdnAttrib+"="+missingGroup+","+groupBaseDn+"\n");
                sb.append("changetype: add\n");
                sb.append("objectclass: "+groupLdapClass+"\n");
                sb.append("objectclass: top\n");
                // do we need "cn"????
                //sb.append("cn: PublicAdmin");
            }
            sb.append("\n");
        }
        if(report.getExtraLdapGroups().size()>0)
        {
            sb.append("******************************************************************\n");
            sb.append("Groups found in LDAP that need to be removed\n");
            sb.append("******************************************************************\n");
            for(String extraGroup : report.getExtraLdapGroups())
            {
                sb.append(extraGroup+"\n");
            }
            sb.append("\n");
            sb.append("------------------------------------------------------------------\n");
            sb.append("LDIF to remove extra groups into LDAP\n");
            sb.append("------------------------------------------------------------------\n");
            for(String extraGroup : report.getExtraLdapGroups())
            {
                sb.append("\n");
                sb.append("dn: "+groupRdnAttrib+"="+extraGroup+","+groupBaseDn+"\n");
                sb.append("changetype: delete\n");
            }
            sb.append("\n");
        }
        if(report.getMissingLdapMembers().size()>0)
        {
            sb.append("******************************************************************\n");
            sb.append("Members missing from LDAP that should be added\n");
            sb.append("******************************************************************\n");
            for(MembershipDifference missingUser : report.getMissingLdapMembers())
            {
                sb.append("missing "+missingUser.getLdapDn()+" in group "+missingUser.getLdapGroup()+"\n");
            }
            sb.append("\n");
            sb.append("------------------------------------------------------------------");
            sb.append("LDIF to add missing users to LDAP");
            sb.append("------------------------------------------------------------------");
            String groupRdn = null;
            for(MembershipDifference missingUser : report.getMissingLdapMembers())
            {
                if(!missingUser.getLdapGroup().equals(groupRdn))
                {
                    groupRdn = missingUser.getLdapGroup();
                    sb.append("\n");
                    sb.append("dn: "+groupRdnAttrib+"="+groupRdn+","+groupBaseDn+"\n");
                    sb.append("changetype: modify\n");
                    sb.append("add: uniqueMember\n");
                }
                sb.append("uniqueMember: "+missingUser.getLdapDn()+"\n");
            }
            sb.append("\n");
        }
        if(report.getExtraLdapMembers().size()>0)
        {
            sb.append("******************************************************************\n");
            sb.append("Members found in LDAP that should be deleted from LDAP or added to Grouper\n");
            sb.append("******************************************************************\n");
            String groupRdn = null;
            for(MembershipDifference extraUser : report.getExtraLdapMembers())
            {
                if(!extraUser.getLdapGroup().equals(groupRdn))
                {
                    groupRdn = extraUser.getLdapGroup();
                    sb.append("\nin group "+extraUser.getLdapGroup()+"\n");
                }
                sb.append("  "+extraUser.getLdapDn()+"\n");
            }
            sb.append("\n");
            sb.append("------------------------------------------------------------------\n");
            sb.append("LDIF to remove extra users to LDAP\n");
            sb.append("------------------------------------------------------------------\n");
            groupRdn = null;
            for(MembershipDifference extraUser : report.getExtraLdapMembers())
            {
                if(!extraUser.getLdapGroup().equals(groupRdn))
                {
                    groupRdn = extraUser.getLdapGroup();
                    sb.append("\n");
                    sb.append("dn: "+groupRdnAttrib+"="+groupRdn+","+groupBaseDn+"\n");
                    sb.append("changetype: modify\n");
                    sb.append("delete: uniqueMember\n");
                }
                sb.append("uniqueMember: "+extraUser.getLdapDn()+"\n");
            }
            sb.append("\n");
            sb.append("------------------------------------------------------------------\n");
            sb.append("Grouper import\n");
            sb.append("------------------------------------------------------------------\n");
            String groupId = null;
            for(MembershipDifference extraUser : report.getExtraLdapMembers())
            {
                if(!extraUser.getGrouperGroup().equals(groupId))
                {
                    groupId = extraUser.getGrouperGroup();
                    sb.append("\n");
                    sb.append("Find this group in Grouper: "+groupId+"\n");
                    sb.append("and import these users:\n");
                }
                sb.append(extraUser.getGrouperPersonId()+"\n");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private void reportExtraLdapGroups(DeltaReport report) throws NamingException
    {
        List<SearchResult> ldapGroups = ldap.search2(groupBaseDn, "(objectClass="+groupLdapClass+")", new String[]{groupRdnAttrib});
        for(SearchResult ldapGroup : ldapGroups)
        {
            String ldapName = ldapGroup.getAttributes().get(groupRdnAttrib).get().toString();
            if(!report.getMatchedLdapGroups().contains(ldapName))
            {
                report.getExtraLdapGroups().add(ldapName);
            }
        }
    }

    
    private void reportFolder(GrouperFolder root, GrouperFolder parent, DeltaReport report) throws NamingException
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
                report.getMissingLdapGroups().add(ldapName);
            }
            else
            {
                report.getMatchedLdapGroups().add(ldapName);
                
                List<String> ldapUsers = gatherLdapGroupMembershipAsLdapDns(ldapGroupMatches);
                List<String> grouperUsers = gatherGrouperGroupMembershipAsLdapDns(group);
                reportMembershipDifferences(ldapName, group, ldapUsers, grouperUsers, report);
            }
        }
        for(GrouperFolder folder : parent.getChildFolders())
        {
            reportFolder(root, folder, report);
        }
    }

    private void reportMembershipDifferences(String ldapName, GrouperGroup group, List<String> ldapUsers, List<String> grouperUsers, DeltaReport report)
    {
        for(String grouperUser : grouperUsers)
        {
            if(!ldapUsers.contains(grouperUser))
                report.getMissingLdapMembers().add(new MembershipDifference(ldapName, group.getFullDisplayName(), grouperUser, extractIdFromDn(grouperUser)));
        }
        for(String ldapUser : ldapUsers)
        {
            if(!grouperUsers.contains(ldapUser))
                report.getExtraLdapMembers().add(new MembershipDifference(ldapName, group.getFullDisplayName(), ldapUser, extractIdFromDn(ldapUser)));
        }
    }
    
    private String extractIdFromDn(String dn)
    {
        return dn.substring(3, 39);
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
