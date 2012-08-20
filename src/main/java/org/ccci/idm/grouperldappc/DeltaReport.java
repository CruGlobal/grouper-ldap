package org.ccci.idm.grouperldappc;

import java.util.ArrayList;
import java.util.List;

import org.ccci.idm.grouperldappc.obj.GroupForSync;
import org.ccci.idm.grouperldappc.obj.MembershipDifference;

public class DeltaReport
{
    List<GroupForSync> extraLdapGroups = new ArrayList<GroupForSync>();
    List<GroupForSync> missingLdapGroups = new ArrayList<GroupForSync>();
    List<String> matchedLdapGroups = new ArrayList<String>();
    
    List<MembershipDifference> missingLdapMembers = new ArrayList<MembershipDifference>();
    List<MembershipDifference> extraLdapMembers = new ArrayList<MembershipDifference>();
    

    public boolean isEmpty()
    {
        return extraLdapGroups.isEmpty() && missingLdapGroups.isEmpty() && missingLdapMembers.isEmpty() && extraLdapMembers.isEmpty();
    }
    
    public List<GroupForSync> getExtraLdapGroups()
    {
        return extraLdapGroups;
    }
    public void setExtraLdapGroups(List<GroupForSync> extraLdapGroups)
    {
        this.extraLdapGroups = extraLdapGroups;
    }
    public List<GroupForSync> getMissingLdapGroups()
    {
        return missingLdapGroups;
    }
    public void setMissingLdapGroups(List<GroupForSync> missingLdapGroups)
    {
        this.missingLdapGroups = missingLdapGroups;
    }
    public List<String> getMatchedLdapGroups()
    {
        return matchedLdapGroups;
    }
    public void setMatchedLdapGroups(List<String> matchedLdapGroups)
    {
        this.matchedLdapGroups = matchedLdapGroups;
    }
    public List<MembershipDifference> getMissingLdapMembers()
    {
        return missingLdapMembers;
    }
    public void setMissingLdapMembers(List<MembershipDifference> missingLdapMembers)
    {
        this.missingLdapMembers = missingLdapMembers;
    }
    public List<MembershipDifference> getExtraLdapMembers()
    {
        return extraLdapMembers;
    }
    public void setExtraLdapMembers(List<MembershipDifference> extraLdapMembers)
    {
        this.extraLdapMembers = extraLdapMembers;
    }
}
