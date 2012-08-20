package org.ccci.idm.grouperldappc.obj;

public class ExternUser
{
    private String id;
    private String fullPath;
    
    public ExternUser(String id, String fullPath)
    {
        super();
        this.id = id;
        this.fullPath = fullPath;
    }
    public String getId()
    {
        return id;
    }
    public void setId(String id)
    {
        this.id = id;
    }
    public String getFullPath()
    {
        return fullPath;
    }
    public void setFullPath(String fullPath)
    {
        this.fullPath = fullPath;
    }
}
