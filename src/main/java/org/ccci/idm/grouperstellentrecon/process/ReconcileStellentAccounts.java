package org.ccci.idm.grouperstellentrecon.process;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.ccci.idm.grouperrecon.ExternalFolder;
import org.ccci.idm.grouperrecon.ExternalGroup;
import org.ccci.idm.grouperrecon.ExternalNode;
import org.ccci.idm.grouperrecon.ReconcileHierarchicalList;
import org.ccci.idm.grouperstellentrecon.service.StellentAccountService;


public class ReconcileStellentAccounts extends ReconcileHierarchicalList
{
    private StellentAccountService stellentService;
    
    public ReconcileStellentAccounts(StellentAccountService stellentService, String grouperUser, String adminUsers, String groupPrefix)
    {
        super(grouperUser, adminUsers, groupPrefix);
        this.stellentService = stellentService;
    }
    
    @Override
    protected List<ExternalNode> getExternalGroups() throws Exception
    {
        List<ExternalNode> retVal = new ArrayList<ExternalNode>();
        List<String> accountList = stellentService.getAccountList();
        Collections.sort(accountList);
        for(String groupName :  accountList)
        {
            //System.out.println("processing: "+groupName);
            String splits[] = groupName.split("-");
            List<ExternalNode> list = retVal;
            for(int i=0; i<splits.length; i++)
            {
                String name = splits[i];
                ExternalFolder folder = null;
                for(ExternalNode f : list)
                {
                    if(f.getName().equals(name)) folder = (ExternalFolder)f;
                }
                if(folder==null)
                {
                    folder = new ExternalFolder(name);
                    list.add(folder);
                    list.add(new ExternalGroup(name+"_R"));
                    list.add(new ExternalGroup(name+"_RWD"));
                }
                list = folder.getChildren();
            }
        }
        retVal.add(new ExternalGroup("UCMAllAccounts"));
        return retVal;
    }
}
