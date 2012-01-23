package org.ccci.idm.grouperstellentrecon.process;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.ccci.idm.grouperrecon.ExternalGroup;
import org.ccci.idm.grouperrecon.ReconcileHierarchicalList;
import org.ccci.idm.grouperstellentrecon.service.StellentAccountService;


public class ReconcileStellentAccounts extends ReconcileHierarchicalList
{
    private StellentAccountService stellentService;
    
    public ReconcileStellentAccounts(StellentAccountService stellentService, String grouperUser, String adminUser, String groupPrefix)
    {
        super(grouperUser, adminUser, groupPrefix);
        this.stellentService = stellentService;
    }
    
    @Override
    protected List<ExternalGroup> getExternalGroups() throws Exception
    {
        List<ExternalGroup> retVal = new ArrayList<ExternalGroup>();
        List<String> accountList = stellentService.getAccountList();
        Collections.sort(accountList);
        for(String groupName :  accountList)
        {
            System.out.println("processing: "+groupName);
            String splits[] = groupName.split("-");
            List<ExternalGroup> list = retVal;
            for(int i=0; i<splits.length; i++)
            {
                String name = splits[i];
                ExternalGroup group = null;
                for(ExternalGroup g : list)
                {
                    if(g.getName().equals(name)) group = g;
                }
                if(group==null)
                {
                    group = new ExternalGroup(name);
                    list.add(group);
                }
                list = group.getChildren();
            }
        }
        return retVal;
    }
}
