package org.ccci.idm.grouperldappc.old;

import org.apache.commons.logging.Log;
import org.ccci.idm.grouper.dao.GrouperDao;
import org.ccci.idm.grouper.dao.GrouperDaoImpl;
import org.ccci.idm.ldap.Ldap;

import edu.internet2.middleware.grouper.util.ConfigItem;
import edu.internet2.middleware.grouper.util.GrouperUtil;

public class LdapProvConBase
{
    protected static final Log LOG = GrouperUtil.getLog(LdapProvConBase.class);
    
    @ConfigItem
    protected String userRdnAttribName = "cn";
    @ConfigItem
    protected String userBaseDn = "CN=Users,CN=idm,DC=cru,DC=org";
    @ConfigItem
    protected String groupRdnAttrib = "cn";
    @ConfigItem
    protected String groupBaseDn = "CN=Other,CN=Groups,CN=idm,DC=cru,DC=org";
    @ConfigItem
    protected String groupLdapClass = "group";
    @ConfigItem
    protected String grouperPrefix = "ccci:itroles:uscore:ldap";
    @ConfigItem
    protected String ldapUsername = "GrouperAdmin";
    @ConfigItem
    protected String ldapPassword = "sdkfbaw34(*Pkjy32";
    @ConfigItem
    protected String ldapUrl = "ldaps://twidma27.net.ccci.org:636";
    @ConfigItem
    protected String flatteningPathSeparatorCharacter = "-";
    @ConfigItem
    protected String computeFromDescr = "false";
    @ConfigItem
    protected String memberAttribName = "member";
    @ConfigItem
    protected String flatten = "false";
    @ConfigItem
    protected String containerRdnAttrib = "cn";
    @ConfigItem
    protected String containerLdapClass = "container";
    
    
    //@ConfigItem
    //protected String groupLdapClasses = "group";
    //@ConfigItem
    //protected String containerLdapClasses = "container";
    
    protected GrouperDao dao;
    protected Ldap ldap;
    
    
    
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
    
    protected String extractIdFromDn(String dn)
    {
        return dn.substring(3, 39);
    }
}
