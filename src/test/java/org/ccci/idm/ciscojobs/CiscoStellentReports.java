package org.ccci.idm.ciscojobs;

import org.ccci.idm.grouperldappc.BasicDeltaReportTask;

import edu.internet2.middleware.grouper.util.ConfigUtil;

public class CiscoStellentReports
{

    /**
     * @param args
     */
    public static void main(String[] args)
    {
        long start = System.currentTimeMillis();
        String customJobName = "ciscoDeltaReport";
        BasicDeltaReportTask task = new BasicDeltaReportTask(customJobName);
        ConfigUtil.readGrouperLoaderConfig(task, "customJob." + customJobName + ".");
        task.run();
        long end = System.currentTimeMillis();
        
        System.out.println("time to run: "+(end-start)+" ms");
        System.out.println("time to run: "+(((double)(end-start))/1000/60)+" min");
    }

}
