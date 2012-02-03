package org.ccci.idm.grouperstellentrecon;


public class StellentAccountReconTaskDemo
{
    public static void main(String[] args)
    {
        StellentAccountReconTask task = new StellentAccountReconTask();
        task.setUrl("http://ucm-qa.ccci.org/ucmqa/idcplg?IdcService=QUERY_DOC_ACCOUNTS&IsSoap=1");
        task.setUsername("stellent.account.recon@ccci.org");
        task.setPassword("aq1sw2de3");
        task.setLoginUrl("https://signin.ccci.org/cas/login");
        task.setUseCas("true");
        task.run();
    }
}
