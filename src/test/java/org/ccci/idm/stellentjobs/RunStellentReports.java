package org.ccci.idm.stellentjobs;

import org.ccci.idm.grouperldappc.RecursiveToFlatDeltaReportTask;

import edu.internet2.middleware.grouper.util.ConfigUtil;

public class RunStellentReports
{

    /**
     * @param args
     */
    public static void main(String[] args)
    {
        long start = System.currentTimeMillis();
        String customJobName = "stellentDeltaReport";
        RecursiveToFlatDeltaReportTask task = new RecursiveToFlatDeltaReportTask();
        ConfigUtil.readGrouperLoaderConfig(task, "customJob." + customJobName + ".");
        task.run();
        
        customJobName = "stellentDeltaReport2";
        task = new RecursiveToFlatDeltaReportTask();
        ConfigUtil.readGrouperLoaderConfig(task, "customJob." + customJobName + ".");
        task.run();
        long end = System.currentTimeMillis();
        
        System.out.println("time to run: "+(end-start)+" ms");
        System.out.println("time to run: "+(((double)(end-start))/1000/60)+" min");
    }

}
