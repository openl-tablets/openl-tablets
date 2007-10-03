package org.openl.rules.ui.repository.beans;

public class XFile {
    private String path;
    private String name;

    public XFile(String path) {
	this.path = path;
	int i = path.lastIndexOf('/');
	name = (i < 0) ? path : path.substring(i + 1);
    }
    
    public String getPath() {
	return path;
    }
    
    public String getName() {
	return name;
    }
}
