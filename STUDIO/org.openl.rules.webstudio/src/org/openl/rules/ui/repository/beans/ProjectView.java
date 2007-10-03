package org.openl.rules.ui.repository.beans;

public class ProjectView extends EntityView {
    private String id;
    private RepositoryView repository;

    public ProjectView(String id, RepositoryView repository) {
	super();
	this.id = id;
	this.repository = repository;
    }
    
    public String duplicateProject() {
	return "duplicateProject";
    }
    
    public String deleteProject() {
	if (repository.deleteProject(id)) {
	    return "success";
	} else {
	    return "failed";
	}
    }
    
    public boolean deleteFolder(String folderId) {
	return repository.deleteFolder(id, folderId);
    }
}
