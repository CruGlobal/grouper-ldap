package org.ccci.idm.grouperldappc;

import java.util.ArrayList;
import java.util.List;

import javax.naming.NamingException;
import javax.naming.directory.SearchResult;

import org.apache.commons.logging.Log;
import org.ccci.idm.grouper.dao.GrouperDao;
import org.ccci.idm.grouper.dao.GrouperDaoImpl;
import org.ccci.idm.grouper.obj.GrouperGroup;
import org.ccci.idm.ldap.Ldap;
import org.ccci.idm.obj.SsoUser;

import edu.internet2.middleware.grouper.util.ConfigItem;
import edu.internet2.middleware.grouper.util.GrouperUtil;


/**
 * @author Nathan.Kopp
 *
 */
public class FlatExistenceDeltaReportTask extends ReportTask
{
    private static final Log LOG = GrouperUtil.getLog(FlatExistenceDeltaReportTask.class);
    
    @ConfigItem
    private String userRdnAttribName = "cn";
    @ConfigItem
    private String userBaseDn = "ou=mpusers,dc=ccci,dc=org";
    @ConfigItem
    private String groupId = "ccci:itroles:uscore:ciscoconferencing:users";
    @ConfigItem
    private String userObjectClass = "Person";
    @ConfigItem
    private String ldapUsername = "cn=Directory Manager";
    @ConfigItem
    private String ldapPassword = "Lakehart1";
    @ConfigItem
    private String ldapUrl = "ldap://hart-a909.ccci.org:58389";
    
    private GrouperDao dao;
    private Ldap ldap;
    
    protected void openConnection() throws Exception
    {
        System.out.println("FlatExistenceDeltaReportTask opening connection");
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
            System.out.println("FlatExistenceDeltaReportTask closing connection");
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
        GrouperGroup group = dao.loadGroup(groupId);
        
        DeltaReport report = new DeltaReport();
        
        createUserReport(userBaseDn, group, report);
        
        String reportStr = generateReport(report);
        
        //System.out.println(reportStr);
        
        sendReport(reportStr);
    }

    private String generateReport(DeltaReport report)
    {
        StringBuffer sb = new StringBuffer();
        sb.append("\n");
        sb.append("******************************************************************\n");
        sb.append(reportName+" for "+systemId+"\n");
        sb.append("******************************************************************\n");
        sb.append("\n\n");
        
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
            for(MembershipDifference missingUser : report.getMissingLdapMembers())
            {
                sb.append("\n");
                sb.append("dn: "+missingUser.getLdapDn()+"\n");
                sb.append("objectClass: inetOrgPerson\n");
                sb.append("objectClass: organizationalPerson\n");
                sb.append("objectClass: Person\n");
                sb.append("objectClass: top\n");
                sb.append("cn: "+extractIdFromDn(missingUser.getLdapDn())+"\n");
                sb.append("sn: "+missingUser.getSsoUser().getUsername()+"\n");
                sb.append("givenName: "+missingUser.getSsoUser().getUsername()+"\n");
                sb.append("mail: "+missingUser.getSsoUser().getUsername()+"\n");
                sb.append("uid: MAUREEN.HORNSTEIN@CCCI.ORG\n");
                sb.append("userPassword: dummy\n\n");
            }
            sb.append("\n");
            sb.append("\n");
            sb.append("------------------------------------------------------------------");
            sb.append("List to remove users from grouper");
            sb.append("------------------------------------------------------------------");
            for(MembershipDifference missingUser : report.getMissingLdapMembers())
            {
                sb.append(extractIdFromDn(missingUser.getLdapDn())+"\n");
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
            sb.append("LDIF to remove extra users from LDAP\n");
            sb.append("------------------------------------------------------------------\n");
            for(MembershipDifference extraUser : report.getExtraLdapMembers())
            {
                groupRdn = extraUser.getLdapGroup();
                sb.append("\n");
                sb.append("dn: "+extraUser.getLdapDn()+"\n");
                sb.append("changetype: delete\n");
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

    
    private void createUserReport(String userBaseDn, GrouperGroup group, DeltaReport report) throws NamingException
    {
        List<String> ldapUsers = gatherLdapUsersAsLdapDns(userBaseDn);
        List<SsoUser> grouperUsersFull = dao.getMembers(group.getFullPath());
        List<String> grouperUsers = gatherGrouperGroupMembershipAsLdapDns(grouperUsersFull);
        reportMembershipDifferences(userBaseDn, group, ldapUsers, grouperUsers, grouperUsersFull, report);
    }

    private List<String> gatherLdapUsersAsLdapDns(String userBaseDn) throws NamingException
    {
        List<SearchResult> ldapUsers1 = ldap.search2(userBaseDn, "(objectClass="+userObjectClass+")", new String[]{userRdnAttribName});
        List<String> ldapUsers = new ArrayList<String>();
        for(SearchResult sr : ldapUsers1)
        {
            String cn = sr.getAttributes().get(userRdnAttribName).get().toString();
            ldapUsers.add((userRdnAttribName+"="+cn+","+userBaseDn).toLowerCase());
        }
        return ldapUsers;
    }

    private void reportMembershipDifferences(String ldapName, GrouperGroup group, List<String> ldapUsers, List<String> grouperUsers, List<SsoUser> grouperUsersFull, DeltaReport report)
    {
        for(SsoUser grouperUser : grouperUsersFull)
        {
            String userDn = userRdnAttribName+"="+grouperUser.getSsoGuid()+","+userBaseDn;
            userDn = userDn.toLowerCase();
            if(!ldapUsers.contains(userDn))
                report.getMissingLdapMembers().add(new MembershipDifference(ldapName, group.getFullDisplayName(), userDn, grouperUser.getSsoGuid(), grouperUser));
        }
        for(String ldapUser : ldapUsers)
        {
            if(!grouperUsers.contains(ldapUser))
                report.getExtraLdapMembers().add(new MembershipDifference(ldapName, group.getFullDisplayName(), ldapUser, extractIdFromDn(ldapUser), null));
        }
    }
    
    private String extractIdFromDn(String dn)
    {
        return dn.substring(userRdnAttribName.length()+1, userRdnAttribName.length()+37);
    }

    private List<String> gatherGrouperGroupMembershipAsLdapDns(List<SsoUser> grouperUsersFull)
    {
        List<String> grouperUsers = new ArrayList<String>();
        for(SsoUser user : grouperUsersFull)
        {
            String userDn = userRdnAttribName+"="+user.getSsoGuid()+","+userBaseDn;
            grouperUsers.add(userDn.toLowerCase());
        }
        return grouperUsers;
    }


}
