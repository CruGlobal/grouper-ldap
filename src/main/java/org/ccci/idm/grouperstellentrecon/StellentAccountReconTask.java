package org.ccci.idm.grouperstellentrecon;

import org.ccci.idm.grouperrecon.ReconciliationTask;
import org.ccci.idm.grouperstellentrecon.process.ReconcileAccounts;

import edu.internet2.middleware.grouper.util.ConfigItem;


public class StellentAccountReconTask extends ReconciliationTask
{
    @ConfigItem
    private String username = "siebel.account.recon@ccci.org";
    @ConfigItem
    private String password = "-------";
    @ConfigItem
    private String url = "http://ucm-qa.ccci.org/ucmqa/idcplg?IdcService=QUERY_DOC_ACCOUNTS&IsSoap=1";
    
    protected Object stellentService = null;
    
    protected void openConnection()
    {
        stellentService = "dummy value";
        reconProc = new ReconcileAccounts(stellentService);
    }
    
    protected void closeConnection()
    {
        try
        {
            stellentService = null;  // close
        }
        catch(Exception e)
        {
            // do nothing;
        }
        stellentService = null;
        reconProc = null;
    }



/*
<?xml version='1.0' encoding='utf-8' ?>
<SOAP-ENV:Envelope xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/">
<SOAP-ENV:Body>
<idc:service xmlns:idc="http://www.stellent.com/IdcService/" IdcService="QUERY_DOC_ACCOUNTS">
<idc:document dUser="NATHAN.KOPP@CCCI.ORG">
<idc:resultset name="UserAttribInfo" TotalRows="1">
<idc:row>
<idc:field name="dUserName">NATHAN.KOPP@CCCI.ORG</idc:field>
<idc:field name="AttributeInfo">account,Shared-LH-IT-DSS,7,account,#all,15,account,UCMAllAccounts,15,account,Private-LH-IT-DSS,7,account,#none,15,account,Web,1,account,Shared,1,role,StaffOnlyConsumer,15,role,UCMSysManager,15,role,sysmanager,15,role,UCMAdmin,15,role,admin,15</idc:field>
</idc:row>
</idc:resultset>
<idc:resultset name="DOCACCOUNT_INFO" TotalRows="139">
<idc:row dDocAccount="Private">
</idc:row>
<idc:row dDocAccount="Private-AIA">

</idc:row>
<idc:row dDocAccount="Private-Campus">
</idc:row>
<idc:row dDocAccount="Private-Campus-FacultyCommons">
</idc:row>
<idc:row dDocAccount="Private-Campus-GreatLakes">
</idc:row>
<idc:row dDocAccount="Private-Campus-GreatPlains">
</idc:row>
<idc:row dDocAccount="Private-Campus-GreaterNW">
</idc:row>
<idc:row dDocAccount="Private-Campus-MidAtlantic">
</idc:row>
<idc:row dDocAccount="Private-Campus-MidSouth">
</idc:row>
<idc:row dDocAccount="Private-Campus-NCO">
</idc:row>

<idc:row dDocAccount="Private-Campus-NCO-HR">
</idc:row>
<idc:row dDocAccount="Private-Campus-NCO-IT">
</idc:row>
<idc:row dDocAccount="Private-Campus-NCO-Media">
</idc:row>
<idc:row dDocAccount="Private-Campus-Northeast">
</idc:row>
<idc:row dDocAccount="Private-Campus-PacificSW">
</idc:row>
<idc:row dDocAccount="Private-Campus-RedRiver">
</idc:row>
<idc:row dDocAccount="Private-Campus-Southeast">
</idc:row>
<idc:row dDocAccount="Private-Campus-StudentVenture">
</idc:row>
<idc:row dDocAccount="Private-Campus-UpperMidWest">

</idc:row>
<idc:row dDocAccount="Private-EmbassyDC">
</idc:row>
<idc:row dDocAccount="Private-EmbassyUN">
</idc:row>
<idc:row dDocAccount="Private-FamilyLife">
</idc:row>
<idc:row dDocAccount="Private-HLIC">
</idc:row>
<idc:row dDocAccount="Private-Impact">
</idc:row>
<idc:row dDocAccount="Private-Jfilm">
</idc:row>
<idc:row dDocAccount="Private-Josh">
</idc:row>
<idc:row dDocAccount="Private-Keynote">
</idc:row>

<idc:row dDocAccount="Private-LH">
</idc:row>
<idc:row dDocAccount="Private-LH-Comm">
</idc:row>
<idc:row dDocAccount="Private-LH-Comm-Photo">
</idc:row>
<idc:row dDocAccount="Private-LH-Comm-Web">
</idc:row>
<idc:row dDocAccount="Private-LH-Dontations">
</idc:row>
<idc:row dDocAccount="Private-LH-Finance">
</idc:row>
<idc:row dDocAccount="Private-LH-Finance-Budget">
</idc:row>
<idc:row dDocAccount="Private-LH-Finance-OAS">
</idc:row>
<idc:row dDocAccount="Private-LH-Finance-OneCard">

</idc:row>
<idc:row dDocAccount="Private-LH-Finance-RiskMgmt">
</idc:row>
<idc:row dDocAccount="Private-LH-FundDev">
</idc:row>
<idc:row dDocAccount="Private-LH-HR">
</idc:row>
<idc:row dDocAccount="Private-LH-HR-Crisis">
</idc:row>
<idc:row dDocAccount="Private-LH-HR-Recruiting">
</idc:row>
<idc:row dDocAccount="Private-LH-HR-SS">
</idc:row>
<idc:row dDocAccount="Private-LH-HR-SS-Healthcare">
</idc:row>
<idc:row dDocAccount="Private-LH-HR-SS-Insurance">
</idc:row>

<idc:row dDocAccount="Private-LH-HR-SS-MHA">
</idc:row>
<idc:row dDocAccount="Private-LH-HR-SS-Retirement">
</idc:row>
<idc:row dDocAccount="Private-LH-HR-USHRDT">
</idc:row>
<idc:row dDocAccount="Private-LH-IT">
</idc:row>
<idc:row dDocAccount="Private-LH-IT-DSS">
</idc:row>
<idc:row dDocAccount="Private-LH-IT-HelpDesk">
</idc:row>
<idc:row dDocAccount="Private-LH-IT-ProjectOffice">
</idc:row>
<idc:row dDocAccount="Private-LH-Legal">
</idc:row>
<idc:row dDocAccount="Private-LH-MPD">

</idc:row>
<idc:row dDocAccount="Private-LH-Private-LHCommunity">
</idc:row>
<idc:row dDocAccount="Private-LH-USSC">
</idc:row>
<idc:row dDocAccount="Private-Marketplace">
</idc:row>
<idc:row dDocAccount="Private-Military">
</idc:row>
<idc:row dDocAccount="Private-VP">
</idc:row>
<idc:row dDocAccount="Private-VP-AmericasOceania">
</idc:row>
<idc:row dDocAccount="Private-VP-EastAsia">
</idc:row>
<idc:row dDocAccount="Private-VP-Europe">
</idc:row>

<idc:row dDocAccount="Private-VP-GlobalCampus">
</idc:row>
<idc:row dDocAccount="Private-VP-GlobalLeaderDev">
</idc:row>
<idc:row dDocAccount="Private-VP-GlobalOps">
</idc:row>
<idc:row dDocAccount="Private-VP-Namestan">
</idc:row>
<idc:row dDocAccount="Private-VP-SouthAsia">
</idc:row>
<idc:row dDocAccount="Private-VP-SouthEastAsia">
</idc:row>
<idc:row dDocAccount="Shared">
</idc:row>
<idc:row dDocAccount="Shared-AIA">
</idc:row>
<idc:row dDocAccount="Shared-Campus">

</idc:row>
<idc:row dDocAccount="Shared-Campus-FacultyCommons">
</idc:row>
<idc:row dDocAccount="Shared-Campus-GreatLakes">
</idc:row>
<idc:row dDocAccount="Shared-Campus-GreatPlains">
</idc:row>
<idc:row dDocAccount="Shared-Campus-GreaterNW">
</idc:row>
<idc:row dDocAccount="Shared-Campus-MidAtlantic">
</idc:row>
<idc:row dDocAccount="Shared-Campus-MidSouth">
</idc:row>
<idc:row dDocAccount="Shared-Campus-NCO">
</idc:row>
<idc:row dDocAccount="Shared-Campus-NCO-HR">
</idc:row>

<idc:row dDocAccount="Shared-Campus-NCO-IT">
</idc:row>
<idc:row dDocAccount="Shared-Campus-NCO-Media">
</idc:row>
<idc:row dDocAccount="Shared-Campus-Northeast">
</idc:row>
<idc:row dDocAccount="Shared-Campus-PacificSW">
</idc:row>
<idc:row dDocAccount="Shared-Campus-RedRiver">
</idc:row>
<idc:row dDocAccount="Shared-Campus-Southeast">
</idc:row>
<idc:row dDocAccount="Shared-Campus-StudentVenture">
</idc:row>
<idc:row dDocAccount="Shared-Campus-UpperMidWest">
</idc:row>
<idc:row dDocAccount="Shared-EmbassyDC">

</idc:row>
<idc:row dDocAccount="Shared-EmbassyUN">
</idc:row>
<idc:row dDocAccount="Shared-FamilyLife">
</idc:row>
<idc:row dDocAccount="Shared-HLIC">
</idc:row>
<idc:row dDocAccount="Shared-Impact">
</idc:row>
<idc:row dDocAccount="Shared-Jfilm">
</idc:row>
<idc:row dDocAccount="Shared-Josh">
</idc:row>
<idc:row dDocAccount="Shared-Keynote">
</idc:row>
<idc:row dDocAccount="Shared-LH">
</idc:row>

<idc:row dDocAccount="Shared-LH-Comm">
</idc:row>
<idc:row dDocAccount="Shared-LH-Comm-Photo">
</idc:row>
<idc:row dDocAccount="Shared-LH-Comm-Web">
</idc:row>
<idc:row dDocAccount="Shared-LH-Donations">
</idc:row>
<idc:row dDocAccount="Shared-LH-Finance">
</idc:row>
<idc:row dDocAccount="Shared-LH-Finance-Budget">
</idc:row>
<idc:row dDocAccount="Shared-LH-Finance-NatlPrograms">
</idc:row>
<idc:row dDocAccount="Shared-LH-Finance-OAS">
</idc:row>
<idc:row dDocAccount="Shared-LH-Finance-OneCard">

</idc:row>
<idc:row dDocAccount="Shared-LH-Finance-RiskMgmt">
</idc:row>
<idc:row dDocAccount="Shared-LH-FundDev">
</idc:row>
<idc:row dDocAccount="Shared-LH-HR">
</idc:row>
<idc:row dDocAccount="Shared-LH-HR-Crisis">
</idc:row>
<idc:row dDocAccount="Shared-LH-HR-Recruiting">
</idc:row>
<idc:row dDocAccount="Shared-LH-HR-SS">
</idc:row>
<idc:row dDocAccount="Shared-LH-HR-SS-Healthcare">
</idc:row>
<idc:row dDocAccount="Shared-LH-HR-SS-Insurance">
</idc:row>

<idc:row dDocAccount="Shared-LH-HR-SS-MHA">
</idc:row>
<idc:row dDocAccount="Shared-LH-HR-SS-Payroll">
</idc:row>
<idc:row dDocAccount="Shared-LH-HR-SS-Retirement">
</idc:row>
<idc:row dDocAccount="Shared-LH-HR-USHRDT">
</idc:row>
<idc:row dDocAccount="Shared-LH-IT">
</idc:row>
<idc:row dDocAccount="Shared-LH-IT-DSS">
</idc:row>
<idc:row dDocAccount="Shared-LH-IT-DSS2">
</idc:row>
<idc:row dDocAccount="Shared-LH-IT-HelpDesk">
</idc:row>
<idc:row dDocAccount="Shared-LH-IT-ProjectOffice">

</idc:row>
<idc:row dDocAccount="Shared-LH-Legal">
</idc:row>
<idc:row dDocAccount="Shared-LH-MPD">
</idc:row>
<idc:row dDocAccount="Shared-LH-Public-LHCommunity">
</idc:row>
<idc:row dDocAccount="Shared-LH-USSC">
</idc:row>
<idc:row dDocAccount="Shared-Marketplace">
</idc:row>
<idc:row dDocAccount="Shared-Military">
</idc:row>
<idc:row dDocAccount="Shared-VP">
</idc:row>
<idc:row dDocAccount="Shared-VP-AmericasOceania">
</idc:row>

<idc:row dDocAccount="Shared-VP-EastAsia">
</idc:row>
<idc:row dDocAccount="Shared-VP-Europe">
</idc:row>
<idc:row dDocAccount="Shared-VP-GlobalCampus">
</idc:row>
<idc:row dDocAccount="Shared-VP-GlobalLeaderDev">
</idc:row>
<idc:row dDocAccount="Shared-VP-GlobalOps">
</idc:row>
<idc:row dDocAccount="Shared-VP-Namestan">
</idc:row>
<idc:row dDocAccount="Shared-VP-SouthAsia">
</idc:row>
<idc:row dDocAccount="Shared-VP-SouthEastAsia">
</idc:row>
<idc:row dDocAccount="Teams">

</idc:row>
<idc:row dDocAccount="Web">
</idc:row>
</idc:resultset>
</idc:document>
</idc:service>
</SOAP-ENV:Body>
</SOAP-ENV:Envelope>
 */
}
