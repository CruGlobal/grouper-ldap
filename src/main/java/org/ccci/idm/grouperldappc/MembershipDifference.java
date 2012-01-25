package org.ccci.idm.grouperldappc;

public class MembershipDifference
{
    public String ldapGroup;
    public String grouperGroup;
    public String ldapDn;
    public String grouperPersonId;
    
    public MembershipDifference(String ldapGroup, String grouperGroup, String ldapDn, String grouperPersonId)
    {
        super();
        this.ldapGroup = ldapGroup;
        this.grouperGroup = grouperGroup;
        this.ldapDn = ldapDn;
        this.grouperPersonId = grouperPersonId;
    }
    public String getLdapGroup()
    {
        return ldapGroup;
    }
    public void setLdapGroup(String ldapGroup)
    {
        this.ldapGroup = ldapGroup;
    }
    public String getGrouperGroup()
    {
        return grouperGroup;
    }
    public void setGrouperGroup(String grouperGroup)
    {
        this.grouperGroup = grouperGroup;
    }
    public String getLdapDn()
    {
        return ldapDn;
    }
    public void setLdapDn(String ldapDn)
    {
        this.ldapDn = ldapDn;
    }
    public String getGrouperPersonId()
    {
        return grouperPersonId;
    }
    public void setGrouperPersonId(String grouperPersonId)
    {
        this.grouperPersonId = grouperPersonId;
    }
}
