package org.ccci.idm.grouperldappc;

import java.util.List;

import javax.naming.NamingException;

import org.ccci.idm.grouperldappc.obj.ExternGroup;
import org.ccci.idm.grouperldappc.obj.ExternUser;
import org.ccci.idm.grouperldappc.obj.GroupForSync;
import org.ccci.idm.grouperldappc.obj.MembershipDifference;
import org.ccci.idm.obj.SsoUser;

public interface ExternalSystemConnector
{

    void init();

    void close() throws NamingException;

    ExternUser computeExternUserFor(SsoUser grouperUser);

    void addUserToGroup(GroupForSync group, SsoUser user) throws Exception;
    void removeUserFromGroup(GroupForSync group, SsoUser user) throws Exception;

    boolean createGroup(GroupForSync group);
    boolean removeGroup(GroupForSync groupForSync);

    List<ExternGroup> loadGroups(String relativePath);
    List<ExternGroup> loadFolders(String relativePath);
    ExternGroup loadGroup(GroupForSync groupForSync);

    String generateCreationScript(List<GroupForSync> missingLdapGroups);
    String generateDeletionScript(List<GroupForSync> extraLdapGroups);
    String generateAddMembersScript(List<MembershipDifference> missingLdapMembers);
    String generateRemoveMembersScript(List<MembershipDifference> extraLdapMembers);

}
