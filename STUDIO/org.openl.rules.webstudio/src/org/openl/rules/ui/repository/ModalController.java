package org.openl.rules.ui.repository;


// it is BEAN!!!
public class ModalController {
    // Add new Project
    private String newProjectName;

    // Copy existing Project
    private String copyProjectFrom;
    private String copyProjectTo;

    // Add new Folder
    private String newFolderName;

    /**
     * Adds new project to a repository.<p>There is no navigation case for this
     * action. But there is one requirement -- page must be reloaded.</p>
     *
     * @return outcome: "success" or "fail"
     */
    public String addProject() {
        //todo: fix
        //boolean success = getContext().getRepositoryHandler().addProject(newProjectName);
        //refresh();

        // not real use, but still...
        return UiConst.OUTCOME_FAILED;
    }

    public void setNewProjectName(String newProjectName) {
        this.newProjectName = newProjectName;
    }

    public String getNewProjectName() {
        // expect null here
        return newProjectName;
    }

    /**
     * Makes copy of existing repository project.<p>Copies from {@link
     * #getCopyProjectFrom()} to {@link #getCopyProjectTo()}</p>
     *
     * @return outcome: "success" or "fail"
     */
    public String copyProject() {
        //todo: fix
        //boolean success = getContext().getRepositoryHandler().copyProject(copyProjectFrom, copyProjectTo);
//        refresh();

        return UiConst.OUTCOME_FAILED;
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

    public void setNewFolderName(String newFolderName) {
        this.newFolderName = newFolderName;
    }

    public String getNewFolderName() {
        // expect null here
        return newFolderName;
    }


}
