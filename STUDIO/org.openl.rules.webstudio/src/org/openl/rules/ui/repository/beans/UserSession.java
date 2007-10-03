package org.openl.rules.ui.repository.beans;

import java.util.List;

public class UserSession {
    private XRepository repository;
    
    public UserSession() {
	repository = new XRepository();
    }
    
    public List<XProject> listProjects() {
	return repository.listProjects();
    }
    
    public XProject getProject(String path) {
	List<XProject> projects = repository.listProjects();
	XProject result = null;
	
	for (XProject project : projects) {
	    if (project.getPath().equals(path)) {
		result = project;
		break;
	    }
	}
	
	return result;
    }
}
