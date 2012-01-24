package org.ccci.idm.grouperldappc;



public abstract class DeltaReportTask implements Runnable
{
    static Object lock = new Object();
    @Override
    public void run()
    {
        synchronized(lock)
        {
            System.out.println("RUNNING DeltaReportTask "+this.getClass().getSimpleName());
            try
            {
                openConnection();
                runReport();
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
            finally
            {
                closeConnection();
            }
        }
    }
    

    protected abstract void closeConnection();
    protected abstract void openConnection() throws Exception;
    protected abstract void runReport() throws Exception;

}
