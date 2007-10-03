package org.openl.rules.ui.repository.beans;

import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;

public class XFolder {
    private String path;
    private String name;
    private TreeMap<String, XFolder> subFolders;
    private TreeMap<String, XFile> files;
    
    public XFolder(String path) {
	this.path = path;
	int i = path.lastIndexOf('/');
	name = (i < 0) ? path : path.substring(i + 1);
	
	subFolders = new TreeMap<String, XFolder>();
	files = new TreeMap<String, XFile>();
    }
    
    public String getPath() {
	return path;
    }
    
    public String getName() {
	return name;
    }
    
    public List<XFolder> getFolders() {
	LinkedList<XFolder> result = new LinkedList<XFolder>();
	
	for (XFolder folder : subFolders.values()) {
	    result.add(folder);
	}
	
	return result;
    }
    
    public List<XFile> getFiles() {
	LinkedList<XFile> result = new LinkedList<XFile>();
	
	for (XFile file : files.values()) {
	    result.add(file);
	}
	
	return result;
    }
    
    public boolean addFolder(XFolder folder) {
	String name = folder.getName();
	
	if (subFolders.containsKey(name)) {
	    return false;
	} else if (files.containsKey(name)) {
	    return false;
	} else {
	    subFolders.put(name, folder);
	    return true;
	}
    }
    
    public boolean addFile(XFile file) {
	String name = file.getName();
	
	if (files.containsKey(name)) {
	    return false;
	} else if (subFolders.containsKey(name)) {
	    return false;
	} else {
	    files.put(name, file);
	    return true;
	}
    }
}
