package org.openl.rules.webstudio.web.repository.upload;

import org.openl.rules.workspace.uw.UserWorkspace;

public abstract class AProjectCreator {
    private String projectName; 
    private UserWorkspace userWorkspace;
    
    public AProjectCreator(String projectName, UserWorkspace userWorkspace) {        
        this.projectName = projectName;
        this.userWorkspace = userWorkspace;
    }
    
    protected String getProjectName() {
        return projectName;
    }
    
    protected UserWorkspace getUserWorkspace() {
        return userWorkspace;
    }
    
    public abstract String createRulesProject();

}
