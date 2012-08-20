package org.ccci.idm.grouperldappc;

import org.junit.Test;

public class DemoDeltaReportStellent
{
    @Test
    public void test1() throws Exception
    {
        BasicDeltaReportTask task = new BasicDeltaReportTask(null);
        task.setFlatten("true");
        task.setComputeFromDescr("true");
        task.setGrouperRoot("ccci:itroles:uscore:stellent:accounts");
        LdapConnector connector = new LdapConnector();
        connector.setGroupBaseDn("CN=Accounts,CN=Stellent,CN=Groups,CN=idm,DC=cru,DC=org");
        connector.init();
        task.setConnector(connector);
        task.run();
    }
}
