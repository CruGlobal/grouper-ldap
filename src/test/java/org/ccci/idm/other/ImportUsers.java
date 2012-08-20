package org.ccci.idm.other;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import org.ccci.idm.grouper.dao.GrouperDao;
import org.ccci.idm.grouper.dao.GrouperDaoImpl;

public class ImportUsers
{

    /**
     * @param args
     */
    public static void main(String[] args)
    {
        GrouperDao dao = new GrouperDaoImpl("nathan.kopp@ccci.org");

        int count = 0;
        try
        {
            BufferedReader input = new BufferedReader(new FileReader("/temp2/staffonlyconsumer-prod-remaining.csv"));
            try
            {
                String line = null; // not declared within while loop
                while ((line = input.readLine()) != null)
                {
                    try
                    {
                        dao.addMember(line, "ccci:itroles:uscore:stellent:roles:StaffOnlyConsumer");
                        System.out.println("added user " + line + " (" + count + ")");
                    }
                    catch (Exception e)
                    {
                        System.out.println("COULD NOT add user " + line + " (" + count + ")");
                    }
                    count++;
                }
            }
            finally
            {
                input.close();
            }
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
        }

    }

}
