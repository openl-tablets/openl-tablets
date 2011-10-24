package org.openl.rules.ui.view;

import org.openl.rules.ui.tree.TreeNodeBuilder;

/**
 * Interface that describes rules tree view mode.
 */
public interface WebStudioViewMode {

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