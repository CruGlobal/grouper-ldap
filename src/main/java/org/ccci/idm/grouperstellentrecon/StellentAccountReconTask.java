package org.ccci.idm.grouperstellentrecon;

import org.ccci.idm.grouperrecon.ReconciliationTask;
import org.ccci.idm.grouperstellentrecon.process.ReconcileAccounts;
import org.ccci.idm.grouperstellentrecon.service.StellentAccountService;

import edu.internet2.middleware.grouper.util.ConfigItem;


/**
 * This class reads the XML from Stellent listing all document "Accounts" and updates
 * a list of groups in grouper so that they correspond to this list.
 * 
 * @author Nathan.Kopp
 *
 */
public class StellentAccountReconTask extends ReconciliationTask
{
    @ConfigItem
    private String username = "siebel.account.recon@ccci.org";
    @ConfigItem
    private String password = "-------";
    @ConfigItem
    private String url = "http://ucm-qa.ccci.org/ucmqa/idcplg?IdcService=QUERY_DOC_ACCOUNTS&IsSoap=1";
    @ConfigItem
    private String loginUrl = "https:/signin.ccci.org/cas/login";
    
    protected StellentAccountService stellentService = null;
    
    protected void openConnection()
    {
        reconProc = new ReconcileAccounts(new StellentAccountService(username, password, url, loginUrl));
    }
    
    protected void closeConnection()
    {
        try
        {
            stellentService.close();
        }
        catch(Exception e)
        {
            // do nothing;
        }
        stellentService = null;
        reconProc = null;
    }


}
