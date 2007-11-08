package org.openl.rules.ui.repository.tree;

import org.openl.rules.ui.repository.UiConst;
import org.openl.rules.workspace.abstracts.Project;
import org.openl.rules.workspace.abstracts.ProjectVersion;
import org.openl.rules.workspace.uw.UserWorkspaceProject;

import java.util.Date;

/**
 * Represents OpenL project in a tree.  
 * 
 * @author Aleh Bykhavets
 *
 */
public class TreeProject extends TreeFolder {

    private boolean isMarked4Deletion;

    private static final long serialVersionUID = -326805891782640894L;

	public TreeProject(long id, String name) {
		super(id, name);
	}
	
	// ------ UI methods ------
	
	@Override
	public String getType() {
		return UiConst.TYPE_PROJECT;
	}
	
	@Override
	public String getIcon() {
		// todo: FIXME: for mockup
		if ("prj1".equals(getName())) return UiConst.ICON_PROJECT_MOD;

        UserWorkspaceProject project = (UserWorkspaceProject) getDataBean();
        if (project.isOpened()) {
            return UiConst.ICON_PROJECT_OPEN;
        }
        
        if (project.isDeleted()) {
            return UiConst.ICON_PROJECT_DELETED;
        }
        if (project.isCheckedOut()) {
            return UiConst.ICON_PROJECT_CHECKED_OUT;
        }

        return UiConst.ICON_PROJECT;
	}

    public Date getCreatedAt() {
        // todo: uncomment when it is implemented
        // return ((Project)getDataBean()).getVersion().getVersionInfo().getCreatedAt();

        return new Date();
    }
    public String getCreatedBy() {
        // todo: uncomment when it is implemented
        //return ((Project)getDataBean()).getVersion().getVersionInfo().getCreatedBy();
        return "god";
    }

    public String getVersion() {
        ProjectVersion projectVersion = ((Project) getDataBean()).getVersion();
        if (projectVersion == null) {
            return "unversioned";
        }
        return projectVersion.getVersionName();
    }

    public boolean isMarked4Deletion() {
        return isMarked4Deletion;
    }

    public void setMarked4Deletion(boolean marked4Deletion) {
        isMarked4Deletion = marked4Deletion;
    }
}
