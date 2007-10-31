package org.openl.rules.ui.repository;

import org.openl.rules.ui.repository.beans.FolderBean;
import org.openl.rules.ui.repository.beans.ProjectBean;

// it is BEAN!!!
public class ModalController extends AbstractDialogController {

    // Add new Project
    private String newProjectName;
    // Copy existing Project
    private String copyProjectFrom;
    private String copyProjectTo;
    // Add new Folder
    private String newFolderName;
    // Modify (any other)
    private MTYPE modifyType;
    
    private static enum MTYPE {
        COMMIT("commit"),
        UPDATE("update"),
        OVERRIDE_AND_UPDATE("overrideAndUpdate"),
        UNDELETE("undelete"),
        ERASE("erase");
        
        private String name;
        
        private MTYPE(String name) {
            this.name = name;
        }
        
        private String getName() {
            return name;
        }
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
        boolean success = getContext().getRepositoryHandler().addProject(newProjectName);

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
        boolean success = getContext().getRepositoryHandler().copyProject(copyProjectFrom, copyProjectTo);

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
        
        Object dataBean = getContext().getRepositoryTreeHandler().getSelected().getDataBean();
        if (dataBean instanceof FolderBean) {
            FolderBean fb = (FolderBean) dataBean;
            success = getContext().getFolderHandler().addFolder(fb, newFolderName);
        } else if (dataBean instanceof ProjectBean) {
            ProjectBean pb = (ProjectBean) dataBean;
            success = getContext().getProjectHandler().addFolder(pb, newFolderName);
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
     * Commit/Update/Override and Update/Undelete/Erase.
     * 
     * @return outcome: "success" or "fail"
     */
    public String modifyProject() {
        if (modifyType == null) {
            return outcome(false);
        }
        
        Object dataBean = getContext().getRepositoryTreeHandler().getSelected().getDataBean();
        ProjectBean pb = (ProjectBean) dataBean;

        switch (modifyType) {
        case COMMIT:
            break;
        case UPDATE:
            break;
        case OVERRIDE_AND_UPDATE:
            break;
        case UNDELETE:
            getContext().getProjectHandler().undelete(pb);
            break;
        case ERASE:
            getContext().getProjectHandler().erase(pb);
            break;

        default:
            // error !!!
            // must not be here
            break;
        }
        
        // FIXME true/false
        return outcome(true);
    }
    
    public String getModifyType() {
        return "Modify";
    }
    
    public void setModifyType(String modifyType) {
        this.modifyType = null;
        
        for (MTYPE type : MTYPE.values()) {
            if (type.getName().equals(modifyType)) {
                this.modifyType = type;
                break;
            }
        }
        
        if (this.modifyType == null) {
            getContext().getMessageQueue().addMessage(new IllegalArgumentException("Illegal modifyType '" + modifyType + "'"));
        }
    }
}
