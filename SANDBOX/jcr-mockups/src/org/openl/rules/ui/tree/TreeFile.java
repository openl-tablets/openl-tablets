package org.openl.rules.ui.tree;

import org.openl.rules.ui.UiConst;

/**
 * Represents OpenL file in a tree.
 * 
 * @author Aleh Bykhavets
 *
 */
public class TreeFile extends AbstractTreeNode {

	private static final long serialVersionUID = -4563895481021883236L;

	public TreeFile(long id, String name) {
		// File cannot have children !!!
		super(id, name, true);
	}
	
	// ------ UI methods ------
	
    /** {@inheritDoc} */
	public String getType() {
		return UiConst.TYPE_FILE;
	}
	
    /** {@inheritDoc} */
	public String getIcon() {
		// file is always leaf node
		return getIconLeaf();
	}
	
    /** {@inheritDoc} */
	public String getIconLeaf() {
		// TODO: different types of files should have own icons
		return UiConst.ICON_FILE;
	}
}
