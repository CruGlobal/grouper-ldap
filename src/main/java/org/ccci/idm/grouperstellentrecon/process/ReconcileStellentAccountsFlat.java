package org.ccci.idm.grouperstellentrecon.process;

import java.util.ArrayList;
import java.util.List;

import org.ccci.idm.grouperrecon.ExternalGroup;
import org.ccci.idm.grouperrecon.ReconcileFlatList;
import org.ccci.idm.grouperstellentrecon.service.StellentAccountService;

public class ReconcileStellentAccountsFlat extends ReconcileFlatList
{
    private static final String DEFAULT_GROUPER_USER = "stellent.account.recon@ccci.org";
    private static final String DEFAULT_ADMIN_USER = "stellent.account.rules@ccci.org";
    private static final String GROUP_PREFIX = "ccci:itroles:uscore:stellent:accounts";
    
    private StellentAccountService stellentService;
      
    public ReconcileStellentAccountsFlat(StellentAccountService stellentService)
    {
        super(DEFAULT_GROUPER_USER, DEFAULT_ADMIN_USER, GROUP_PREFIX);
        this.stellentService = stellentService;
    }
    
    @Override
    protected List<ExternalGroup> getExternalGroups() throws Exception
    {
        List<ExternalGroup> retVal = new ArrayList<ExternalGroup>();
        for(String groupName : stellentService.getAccountList() )
        {
            retVal.add(new ExternalGroup(groupName));
        }
        return retVal;
    }
}
