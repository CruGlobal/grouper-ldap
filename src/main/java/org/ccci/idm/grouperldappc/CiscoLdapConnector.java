package org.ccci.idm.grouperldappc;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.naming.NamingException;
import javax.naming.directory.SearchResult;
import javax.xml.namespace.QName;

import org.apache.commons.logging.Log;
import org.ccci.idm.creds.client.CredentialsWebService;
import org.ccci.idm.creds.client.CredentialsWebServiceService;
import org.ccci.idm.grouper.dao.GrouperDao;
import org.ccci.idm.grouper.dao.GrouperDaoImpl;
import org.ccci.idm.grouperldappc.obj.ExternGroup;
import org.ccci.idm.grouperldappc.obj.ExternUser;
import org.ccci.idm.grouperldappc.obj.GroupForSync;
import org.ccci.idm.grouperldappc.obj.MembershipDifference;
import org.ccci.idm.ldap.Ldap;
import org.ccci.idm.obj.SsoUser;
import org.ccci.util.Exceptions;

import edu.internet2.middleware.grouper.SubjectFinder;
import edu.internet2.middleware.grouper.util.ConfigItem;
import edu.internet2.middleware.grouper.util.GrouperUtil;
import edu.internet2.middleware.subject.Subject;

public class CiscoLdapConnector implements ExternalSystemConnector
{
    private static final Log LOG = GrouperUtil.getLog(CiscoLdapConnector.class);
    
    protected Ldap ldap;
    private CredentialsWebService service;
    private GrouperDao dao;
    
    @ConfigItem
    private String userRdnAttribName = "cn";
    @ConfigItem
    private String userBaseDn = "ou=mpusers,dc=ccci,dc=org";
    @ConfigItem
    private String userObjectClass = "Person";
    @ConfigItem
    private String ldapUsername = "cn=Directory Manager";
    @ConfigItem
    private String ldapPassword = "Lakehart1";
    @ConfigItem
    private String ldapUrl = "ldap://hart-a909.ccci.org:58389";
    @ConfigItem
    private String usernameAttribName = "uid";
    @ConfigItem
    private String userLdapClasses = "inetOrgPerson,organizationalPerson,Person";
    @ConfigItem
    private String passwordAttribName = "userPassword";
    
    @ConfigItem
    private String credsServiceWsdl = "https://cas.ccci.org/password-services/creds?wsdl";
    @ConfigItem
    private String credServiceClientId = null;
    @ConfigItem
    private String credServiceClientKey = null;
    
    
    @Override
    public void init()
    {
        try
        {
            ldap = new Ldap(ldapUrl,ldapUsername,ldapPassword);
            
            if (dao == null)
            {
                dao = new GrouperDaoImpl(null);
                CredentialsWebServiceService locator = new CredentialsWebServiceService(new URL(credsServiceWsdl), new QName("http://webservice.password.idm.ccci.org/", "CredentialsWebServiceService"));
                service = locator.getCredentialsWebServicePort();
            }
        }
        catch(Throwable t)
        {
            throw Exceptions.wrapDontThrow(t);
        }
    }

    @Override
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

    @Override
    public ExternUser computeExternUserFor(SsoUser grouperUser)
    {
        return new ExternUser(grouperUser.getSsoGuid(), userRdnAttribName+"="+grouperUser.getSsoGuid()+","+userBaseDn);
    }

    @Override
    public void addUserToGroup(GroupForSync group, SsoUser user) throws Exception
    {
        Subject grouperSubject = SubjectFinder.findById(user.getSsoGuid(), false);
        
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
        
        if(credServiceClientId!=null) service.syncCredentials(credServiceClientId, credServiceClientKey, user.getSsoGuid(), "cisco_sunds");
        
        LOG.debug("sent password to cisco_sunds for user: "+user.getSsoGuid()+","+user.getUsername());
    }

    @Override
    public void removeUserFromGroup(GroupForSync group, SsoUser user) throws Exception
    {
        ldap.deleteEntity(computeExternUserFor(user).getFullPath());
    }

    @Override
    public boolean createGroup(GroupForSync group)
    {
        return false;
    }

    @Override
    public boolean removeGroup(GroupForSync groupForSync)
    {
        return false;
    }

    @Override
    public List<ExternGroup> loadGroups(String relativePath)
    {
        return new ArrayList<ExternGroup>(0);
    }

    @Override
    public List<ExternGroup> loadFolders(String relativePath)
    {
        return new ArrayList<ExternGroup>(0);
    }

    @Override
    public ExternGroup loadGroup(GroupForSync groupForSync)
    {
        try
        {
            ExternGroup eg = new ExternGroup("User List", userBaseDn, userBaseDn);
            
            List<SearchResult> ldapUsers1 = ldap.search2(userBaseDn, "(objectClass="+userObjectClass+")", new String[]{userRdnAttribName});
            for(SearchResult sr : ldapUsers1)
            {
                String cn = sr.getAttributes().get(userRdnAttribName).get().toString();
                eg.addUser(cn, (userRdnAttribName+"="+cn+","+userBaseDn).toLowerCase());
            }
            return eg;
        }
        catch(Exception e)
        {
            throw Exceptions.wrapDontThrow(e);
        }
    }

    @Override
    public String generateCreationScript(List<GroupForSync> missingLdapGroups)
    {
        return null;
    }

    @Override
    public String generateDeletionScript(List<GroupForSync> extraLdapGroups)
    {
        return null;
    }

    @Override
    public String generateAddMembersScript(List<MembershipDifference> missingLdapMembers)
    {
        StringBuffer sb = new StringBuffer();
        for(MembershipDifference missingUser : missingLdapMembers)
        {
            sb.append("\n");
            sb.append("dn: "+missingUser.getExternUser().getFullPath()+"\n");
            sb.append("objectClass: inetOrgPerson\n");
            sb.append("objectClass: organizationalPerson\n");
            sb.append("objectClass: Person\n");
            sb.append("objectClass: top\n");
            sb.append("cn: "+missingUser.getExternUser().getId()+"\n");
            sb.append("sn: "+missingUser.getSsoUser().getUsername()+"\n");
            sb.append("givenName: "+missingUser.getSsoUser().getUsername()+"\n");
            sb.append("mail: "+missingUser.getSsoUser().getUsername()+"\n");
            sb.append("uid: MAUREEN.HORNSTEIN@CCCI.ORG\n");
            sb.append("userPassword: dummy\n\n");
        }
        return sb.toString();
    }

    @Override
    public String generateRemoveMembersScript(List<MembershipDifference> extraLdapMembers)
    {
        StringBuffer sb = new StringBuffer();
        for(MembershipDifference diff : extraLdapMembers)
        {
            sb.append("\n");
            sb.append("dn: "+diff.getExternUser().getFullPath()+"\n");
            sb.append("changetype: delete\n");
        }
        return sb.toString();
    }

}
