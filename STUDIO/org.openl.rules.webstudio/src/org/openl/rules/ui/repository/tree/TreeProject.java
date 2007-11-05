package org.openl.rules.ui.repository.tree;

import org.openl.rules.ui.repository.UiConst;
import org.openl.rules.workspace.abstracts.Project;

import java.util.Date;

/**
 * Represents OpenL project in a tree.  
 * 
 * @author Aleh Bykhavets
 *
 */
public class TreeProject extends TreeFolder {

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
		// FIXME: for mockup
		if ("prj1".equals(getName())) return UiConst.ICON_PROJECT_MOD;
		
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
}
