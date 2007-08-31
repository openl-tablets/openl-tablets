package org.openl.rules.ui.tree;

import org.openl.rules.ui.UiConst;

/**
 * Represents OpenL folder in a tree.
 * 
 * @author Aleh Bykhavets
 *
 */
public class TreeFolder extends AbstractTreeNode {

	private static final long serialVersionUID = -8236498990436429491L;
	
	public TreeFolder (long id, String name) {
		super(id, name);
	}
	
	// ------ UI methods ------
	
    /** {@inheritDoc} */
	public String getType() {
		return UiConst.TYPE_FOLDER;
	}
	
    /** {@inheritDoc} */
	public String getIcon() {
		// FIXME: for mockup
		if ("rules".equals(getName()) && isLeaf()==false) return UiConst.ICON_FOLDER_MOD;

		return UiConst.ICON_FOLDER;
	}
	
    /** {@inheritDoc} */
	public String getIconLeaf() {
		// in both cases we use the same icons
		return getIcon();
	}
}
