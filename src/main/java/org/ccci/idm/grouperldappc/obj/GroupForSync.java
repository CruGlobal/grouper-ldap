package org.ccci.idm.grouperldappc.obj;

public class GroupForSync
{
    private String shortName;
    private String relativePath;
    
    public GroupForSync(String shortName, String relativePath)
    {
        super();
        this.shortName = shortName;
        this.relativePath = relativePath;
    }

    public String getFullName()
    {
        if(relativePath==null) return getShortName();
        return getRelativePath()+":"+getShortName();
    }

    public String getShortName()
    {
        return shortName;
    }
    public void setShortName(String name)
    {
        this.shortName = name;
    }
    public String getRelativePath()
    {
        return relativePath;
    }
    public void setRelativePath(String containerName)
    {
        this.relativePath = containerName;
    }

}
