package org.openl.rules.ui.repository;

import org.openl.rules.ui.repository.beans.FolderBean;
import org.openl.rules.ui.repository.beans.ProjectBean;

// it is BEAN!!!
public class ModalHandler {
    private Context context;
    // Add new Project
    private String newProjectName;
    // Copy existing Project
    private String copyProjectFrom;
    private String copyProjectTo;
    // Add new Folder
    private String newFolderName;

    public void setContext(Context context) {
        this.context = context;
    }
    
    /**
     * Adds new project to a repository.
     * <p>
     * There is no navigation case for this action.
     * But there is one requirement -- page must be reloaded.
     * 
     * @return outcome: "success" or "fail"
     */
    public String addProject() {
        boolean success = context.getRepositoryHandler().addProject(newProjectName);

        refresh();
        
        // not real use, but still...
        return outcome(success);
    }
    
    public void setNewProjectName(String newProjectName) {
        this.newProjectName = newProjectName;
    }
    
    public String getNewProjectName() {
        // expect null here
        return newProjectName;
    }
    
    /**
     * Makes copy of existing repository project.
     * <p>
     * Copies from {@link #getCopyProjectFrom()} to {@link #getCopyProjectTo()}
     * 
     * @return outcome: "success" or "fail"
     */
    public String copyProject() {
        boolean success = context.getRepositoryHandler().copyProject(copyProjectFrom, copyProjectTo);

        refresh();
        
        return outcome(success);
    }
    
    public void setCopyProjectFrom(String copyProjectFrom) {
        this.copyProjectFrom = copyProjectFrom;
    }

    public void setCopyProjectTo(String copyProjectTo) {
        this.copyProjectTo = copyProjectTo;
    }

    public String getCopyProjectFrom() {
        // expect null here
        return copyProjectFrom;
    }

    public String getCopyProjectTo() {
        // expect null here
        return copyProjectTo;
    }
    
    /**
     * Adds new folder to active node.
     * 
     * @return outcome: "success" or "fail"
     */
    public String addFolder() {
        boolean success;
        
        Object dataBean = context.getRepositoryTreeHandler().getSelected().getDataBean();
        if (dataBean instanceof FolderBean) {
            FolderBean fb = (FolderBean) dataBean;
            success = context.getFolderHandler().addFolder(fb, newFolderName);
        } else if (dataBean instanceof ProjectBean) {
            ProjectBean pb = (ProjectBean) dataBean;
            success = context.getProjectHandler().addFolder(pb, newFolderName);
        } else {
            // error?
            success = false;
        }
        
        refresh();
        
        return outcome(success);
    }
    
    public void setNewFolderName(String newFolderName) {
        this.newFolderName = newFolderName;
    }

    public String getNewFolderName() {
        // expect null here
        return newFolderName;
    }

    /**
     * Modifies project (active node).
     * Commit/Update/Override and Update.
     * 
     * @return outcome: "success" or "fail"
     */
    public String modifyProject() {
        return outcome(false);
    }
    
    // ------ private ------
    
    private String outcome(boolean success) {
        return (success ? "success" : "fail");
    }
    
    private void refresh() {
        context.refresh();
    }
}
