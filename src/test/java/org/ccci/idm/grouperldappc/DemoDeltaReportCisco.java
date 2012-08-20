package org.ccci.idm.grouperldappc;

import org.junit.Test;

public class DemoDeltaReportCisco
{
    @Test
    public void test1() throws Exception
    {
        BasicDeltaReportTask task = new BasicDeltaReportTask(null);
        task.setGrouperRoot("ccci:itroles:uscore:ciscoconferencing:users");
        CiscoLdapConnector connector = new CiscoLdapConnector();
        connector.init();
        task.setConnector(connector);
        task.run();
    }
}
