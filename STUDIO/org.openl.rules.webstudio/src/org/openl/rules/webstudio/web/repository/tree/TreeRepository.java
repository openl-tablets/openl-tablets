package org.openl.rules.webstudio.web.repository.tree;

import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.webstudio.filter.IFilter;
import org.openl.rules.webstudio.web.repository.UiConst;

/**
 * Represents Repository in a tree. Repository is a root element.
 *
 * @author Aleh Bykhavets
 *
 */
public class TreeRepository extends TreeFolder {

    private static final long serialVersionUID = -4465731820834289469L;

    private String type;

    public TreeRepository(String id, String name, IFilter<AProjectArtefact> filter, String type) {
        super(id, name, filter);
        this.type = type;
    }

    // ------ UI methods ------

    @Override
    public String getIcon() {
        return UiConst.ICON_REPOSITORY;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public void refresh() {
        // Don't clear elements. It will make repository node empty because the field "data" for TreeRepository is null.
        // Content of this class is managed outside.
    }
}
