package org.openl.rules.ui.repository.beans;

import java.util.LinkedList;
import java.util.List;

public class RepositoryView {
    private UserSession session;
    
    public void setUserSession(UserSession session) {
        this.session = session;
    }

    public boolean deleteProject(String projectId) {
	XProject project = session.getProject(projectId);
	if (project == null) {
	    return false;
	} else {
	    project.delete();
	    return true;
	}
    }
    
    public boolean deleteFolder(String projectId, String folderId) {
	return false;
    }
    
    public boolean deleteFile(String projectId, String folderId, String fileId) {
	return false;
    }
    
    public List<ProjectView> getProjects() {
	LinkedList<ProjectView> result = new LinkedList<ProjectView>();
	
	List<XProject> projects = session.listProjects();
	for (XProject project : projects) {
	    ProjectView prj = new ProjectView(project.getPath(), this);
	    prj.setName(project.getName());
	    
	    XVersion ver = project.getBaseVersion();
	    prj.setVersion(ver.getName());
//	    prj.setLastModified(lastModified);
	    prj.setLastModifiedBy(ver.getCreatedBy().getName());
	    
	    result.add(prj);
	}
	
	return result;
    }
}
