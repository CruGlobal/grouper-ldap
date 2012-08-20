package org.ccci.idm.grouperldappc.obj;

import java.util.ArrayList;
import java.util.List;

public class ExternGroup
{
    private String shortName;
    private String parentPath;
    private String fullPath;
    private List<ExternUser> members;
    
    public ExternGroup(String name, String containerName, String fullName)
    {
        super();
        this.shortName = name;
        this.parentPath = containerName;
        this.fullPath = fullName;
    }


    public void addUser(String id, String fullPath)
    {
        if(members==null) members = new ArrayList<ExternUser>();
        members.add(new ExternUser(id, fullPath));
    }


    public void ensureMemberList()
    {
        if(members==null) members = new ArrayList<ExternUser>();
    }
    
    public String getShortName()
    {
        return shortName;
    }
    public void setShortName(String name)
    {
        this.shortName = name;
    }
    public String getParentPath()
    {
        return parentPath;
    }
    public void setParentPath(String containerName)
    {
        this.parentPath = containerName;
    }
    public String getFullPath()
    {
        return fullPath;
    }
    public void setFullPath(String fullName)
    {
        this.fullPath = fullName;
    }

    public List<ExternUser> getMembers()
    {
        return members;
    }

    public void setMembers(List<ExternUser> members)
    {
        this.members = members;
    }
}
