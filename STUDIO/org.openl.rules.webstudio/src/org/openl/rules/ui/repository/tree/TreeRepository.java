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
    
    private String type;

    public TreeRepository(long id, String name, String type) {
        super(id, name);
        this.type = type;
    }

    // ------ UI methods ------

    @Override
    public String getType() {
        return type;
    }

    @Override
    public String getIcon() {
        return UiConst.ICON_REPOSITORY;
    }
}
