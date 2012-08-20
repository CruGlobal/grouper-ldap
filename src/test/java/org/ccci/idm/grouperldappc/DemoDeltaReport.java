package org.ccci.idm.grouperldappc;

import org.junit.Test;

public class DemoDeltaReport
{
    @Test
    public void test1() throws Exception
    {
        BasicDeltaReportTask task = new BasicDeltaReportTask(null);
        task.run();
    }
}
