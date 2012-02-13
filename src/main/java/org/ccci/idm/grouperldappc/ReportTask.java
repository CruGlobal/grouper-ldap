package org.ccci.idm.grouperldappc;

import org.ccci.util.mail.EmailAddress;
import org.ccci.util.mail.MailMessage;
import org.ccci.util.mail.MailMessageFactory;

import edu.internet2.middleware.grouper.util.ConfigItem;



public abstract class ReportTask implements Runnable
{
    @ConfigItem
    String smtpHost = "smtp1.ccci.org";
    @ConfigItem
    String reportRecipients = "nathan.kopp@ccci.org";
    @ConfigItem
    String reportSender = "itm-test@ccci.org";
    @ConfigItem
    String systemId = "IdM Test";
    @ConfigItem
    String reportName = "Grouper-LDAP Comparison Report";

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


    protected void sendReport(String reportStr) throws Exception
    {
        MailMessage mailMessage = new MailMessageFactory(smtpHost).createApplicationMessage();
    
        String to[] = reportRecipients.split(",");
        for (String address : to)
        {
            //System.out.println("Sending report to ["+address.trim()+"] from ["+reportSender+"] using ["+smtpHost+"]");
            mailMessage.addTo(EmailAddress.valueOf(address.trim()), "");
        }
    
        mailMessage.setFrom(EmailAddress.valueOf(reportSender), systemId);
    
        mailMessage.setMessage(reportName+" for "+systemId, reportStr, false);
        
        mailMessage.sendToAll();
    }

}
