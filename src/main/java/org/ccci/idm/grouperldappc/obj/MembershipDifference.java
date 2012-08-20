package org.ccci.idm.grouperldappc.obj;

import org.ccci.idm.grouper.obj.GrouperGroup;
import org.ccci.idm.obj.SsoUser;

public class MembershipDifference
{
    public ExternGroup externGroup;
    public GrouperGroup grouperGroup;
    public ExternUser externUser;
    public SsoUser ssoUser;
    
    public MembershipDifference(ExternGroup externGroup, GrouperGroup grouperGroup, ExternUser externUser, SsoUser ssoUser)
    {
        super();
        this.externGroup = externGroup;
        this.grouperGroup = grouperGroup;
        this.externUser = externUser;
        this.ssoUser = ssoUser;
    }
    public ExternGroup getExternGroup()
    {
        return externGroup;
    }
    public void setExternGroup(ExternGroup ldapGroup)
    {
        this.externGroup = ldapGroup;
    }
    public GrouperGroup getGrouperGroup()
    {
        return grouperGroup;
    }
    public void setGrouperGroup(GrouperGroup grouperGroup)
    {
        this.grouperGroup = grouperGroup;
    }
    public SsoUser getSsoUser()
    {
        return ssoUser;
    }
    public void setSsoUser(SsoUser ssoUser)
    {
        this.ssoUser = ssoUser;
    }
    public ExternUser getExternUser()
    {
        return externUser;
    }
    public void setExternUser(ExternUser externUser)
    {
        this.externUser = externUser;
    }
}
