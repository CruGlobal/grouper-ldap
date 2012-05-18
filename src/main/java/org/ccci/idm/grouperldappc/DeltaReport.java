package org.ccci.idm.grouperldappc;

import java.util.ArrayList;
import java.util.List;

public class DeltaReport
{
    List<String> extraLdapGroups = new ArrayList<String>();
    List<String> missingLdapGroups = new ArrayList<String>();
    List<String> matchedLdapGroups = new ArrayList<String>();
    
    List<MembershipDifference> missingLdapMembers = new ArrayList<MembershipDifference>();
    List<MembershipDifference> extraLdapMembers = new ArrayList<MembershipDifference>();
    

    public boolean isEmpty()
    {
        return extraLdapGroups.isEmpty() && missingLdapGroups.isEmpty() && missingLdapMembers.isEmpty() && extraLdapMembers.isEmpty();
    }
    
    public List<String> getExtraLdapGroups()
    {
        return extraLdapGroups;
    }
    public void setExtraLdapGroups(List<String> extraLdapGroups)
    {
        this.extraLdapGroups = extraLdapGroups;
    }
    public List<String> getMissingLdapGroups()
    {
        return missingLdapGroups;
    }
    public void setMissingLdapGroups(List<String> missingLdapGroups)
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
