package org.openl.rules.webstudio.web.repository.upload;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.common.ProjectException;
import org.openl.rules.workspace.uw.UserWorkspace;

public abstract class AProjectCreator {
    
    private final Log log = LogFactory.getLog(AProjectCreator.class);
    
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
    
    /**
     * 
     * @return error message that had occured during the project creation. In other case null. 
     */
    public String createRulesProject() {
        String errorMessage = null;
        RulesProjectBuilder projectBuilder = null;
        try {
            projectBuilder = getProjectBuilder();
            projectBuilder.save();
            
            if (projectBuilder.getProject().getArtefacts().size() == 0) {
                projectBuilder.getProject().delete();
                FacesUtils.addErrorMessage("");
            }
            
            projectBuilder.getProject().edit();
        } catch (Exception e) {
            if (projectBuilder != null) {
                projectBuilder.cancel();
            }
            
            log.error("Error creating project.", e);
            errorMessage = e.getMessage();
            
            // add detailed information
            Throwable cause = e.getCause();
            
            if (cause != null) {
                while (cause.getCause() != null) {
                    cause = cause.getCause();
                }
                
                errorMessage += " Cause: " + cause.getMessage();
            }
        }
        return errorMessage;
    }
    
    protected abstract RulesProjectBuilder getProjectBuilder() throws ProjectException ;

    public abstract void destroy();

}
