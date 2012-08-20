package org.ccci.idm.grouperldappc;

import org.junit.Test;

import edu.internet2.middleware.grouper.changeLog.provisioning.ChangeEvent;

public class DemoGroupConnector
{
    @Test
    public void test1() throws Exception
    {
        BasicEventConnector connector = new BasicEventConnector();
        ChangeEvent event = new ChangeEvent();
        
        event.setEventType(ChangeEvent.ChangeEventType.GROUP_ADD.toString());
        event.setName("ccci:itroles:uscore:ldap:relay-operators:itg-relay-operators");
        connector.dispatchEvent(event);

        event = new ChangeEvent();
        event.setEventType(ChangeEvent.ChangeEventType.MEMBERSHIP_ADD.toString());
        event.setGroupName("ccci:itroles:uscore:ldap:relay-operators:itg-relay-operators");
        event.setSubjectId("ED37F5C6-E154-C6BF-38D0-1E4C78DF6BDC");
        connector.dispatchEvent(event);
    }
}
