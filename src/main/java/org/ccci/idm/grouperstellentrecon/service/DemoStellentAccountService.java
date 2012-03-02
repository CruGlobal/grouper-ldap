package org.ccci.idm.grouperstellentrecon.service;

import java.util.List;

public class DemoStellentAccountService
{

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception
    {
        String username = "stellent.account.recon@ccci.org";
        String password = "aq1sw2de3";
        String url = "http://ucm-qa.ccci.org/ucmqa/idcplg?IdcService=QUERY_DOC_ACCOUNTS&IsSoap=1";
        String loginUrl = "https://signin.ccci.org/cas/login";
        boolean useCas = true;
        /*
        String username = "rtcarlson";
        String password = "idc";
        String url = "http://ucm-dev.ccci.org/ucmdev/idcplg?IdcService=QUERY_DOC_ACCOUNTS&IsSoap=1";
        String loginUrl = null;
        boolean useCas = false;
        */
        StellentAccountService svc = new StellentAccountService(username, password, url, loginUrl, useCas);
        
        List<String> accts = svc.getAccountList();
        for(String acct : accts)
        {
            System.out.println(acct);
        }
    }

}
