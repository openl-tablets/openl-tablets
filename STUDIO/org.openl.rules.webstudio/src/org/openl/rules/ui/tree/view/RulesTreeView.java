package org.openl.rules.ui.tree.view;

import org.openl.rules.ui.tree.TreeNodeBuilder;

/**
 * Interface that describes rules tree view mode.
 */
public interface RulesTreeView {

    String getName();

    String getDisplayName();

    String getDescription();

    /**
     * Gets array of tree node builders.
     *
     * @return tree node builders
     */
    TreeNodeBuilder<Object>[] getBuilders();

}