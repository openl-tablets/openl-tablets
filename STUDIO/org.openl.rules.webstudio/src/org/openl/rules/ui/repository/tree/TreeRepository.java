package org.openl.rules.ui.repository.tree;

import org.openl.rules.ui.repository.UiConst;

/**
 * Represents Repository in a tree.
 * Repository is a root element.  
 * 
 * @author Aleh Bykhavets
 *
 */
public class TreeRepository extends TreeFolder {
	
	private static final long serialVersionUID = -4465731820834289469L;

	public TreeRepository(long id, String name) {
		super(id, name);
	}

	// ------ UI methods ------

	@Override
	public String getType() {
		return UiConst.TYPE_REPOSITORY;
	}
	
	@Override
	public String getIcon() {
		return UiConst.ICON_REPOSITORY;
	}
}
