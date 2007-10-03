package org.openl.rules.ui.repository.beans;

public class XProject {
    private String path;
    private String name;
    private XFolder rootFolder;
    private XRepository parent;
    private XVersionHistory versionHistory;
    
    public XProject(String path, String name) {
	this.path = path;
	this.name = name;
	
	rootFolder = new XFolder("files");
    }
    
    public String getPath() {
	return path;
    }
    
    public String getName() {
	return name;
    }
    
    public XFolder getRootFolder() {
	return rootFolder;
    }
    
    public void setParent(XRepository parent) {
	this.parent = parent;
    }
    
    public XVersionHistory getVersionHistory() {
	if (versionHistory == null) {
	    versionHistory = new XVersionHistory();
	}
	return versionHistory;
    }
    
    public XVersion getBaseVersion() {
	return getVersionHistory().getBaseVersion();
    }

    public void delete() {
	parent.delete(this);
    }
}
