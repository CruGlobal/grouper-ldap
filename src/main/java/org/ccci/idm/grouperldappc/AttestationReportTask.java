package org.ccci.idm.grouperldappc;

import java.util.List;

import org.apache.commons.logging.Log;
import org.ccci.idm.grouper.dao.GrouperDao;
import org.ccci.idm.grouper.dao.GrouperDaoImpl;
import org.ccci.idm.grouper.obj.GrouperMembership;

import edu.internet2.middleware.grouper.util.ConfigItem;
import edu.internet2.middleware.grouper.util.GrouperUtil;


/**
 * @author Nathan.Kopp
 *
 */
public class AttestationReportTask extends ReportTask
{
    private static final Log LOG = GrouperUtil.getLog(AttestationReportTask.class);
    
    private String groupId = "ccci:itroles:uscore:stellent:roles:StaffOnlyConsumer";
    @ConfigItem
    private String attestationUser = "stellent.rules@ccci.org";
    @ConfigItem
    
    private GrouperDao dao;
    
    protected void openConnection() throws Exception
    {
        System.out.println("AttestationReportTask opening connection");
        createDaoIfNecessary();
    }
    
    private void createDaoIfNecessary()
    {
        if (dao == null)
        {
            dao = new GrouperDaoImpl(null);
        }
    }
    
    protected void closeConnection()
    {
        try
        {
            System.out.println("AttestationReportTask closing connection");
        }
        catch(Exception e)
        {
            // do nothing;
        }
    }

    @Override
    protected void runReport() throws Exception
    {
        List<GrouperMembership> memberships = dao.getMemberships(groupId);
        
        String reportStr = "";
        int totalCount = 0;
        int exceptionCount = 0;
        for(GrouperMembership m : memberships)
        {
            if(!m.getAttester().equalsIgnoreCase(attestationUser))
            {
                reportStr += "  "+m.getMember()+" membership attested by "+m.getAttester()+" \n";
                exceptionCount++;
            }
            totalCount++;
        }
        String reportHeader = "Attestation Report\n";
        reportHeader += "Expected attestor: "+attestationUser;
        reportHeader += "\n\nExceptions: "+exceptionCount+"\nTotal:"+totalCount+"\n\n";
        
        reportStr = reportHeader + reportStr;
        
        if(exceptionCount>0) sendReport(reportStr);
    }


}
