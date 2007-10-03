package org.openl.rules.ui.repository.beans;

public class FolderView extends EntityView {
    private String id;
    private ProjectView project;
    
    
    public FolderView(String id, ProjectView project) {
	super();
	this.id = id;
	this.project = project;
    }


    public String deleteFolder() {
	if (project.deleteFolder(id)) {
	    return "success";
	} else {
	    return "failed";
	}
    }
}
