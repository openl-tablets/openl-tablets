/**
 * Created Jan 25, 2007
 */
package org.openl.rules.ui.view;

import org.openl.base.INamedThing;
import org.openl.base.NamedThing;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.project.model.Module;
import org.openl.rules.ui.tree.TreeNodeBuilder;

/**
 * Base class that describes view mode in WebStudio application.
 */
public abstract class WebStudioViewMode extends NamedThing {

    protected String displayName;
    protected String description;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDisplayName(int mode) {

        switch (mode) {
            case INamedThing.SHORT:
                return getName();
            case INamedThing.REGULAR:
                return displayName;
            case INamedThing.LONG:
                return description;
            default:
                return null;
        }
    }

    /**
     * Gets display name.
     * 
     * @param module OpenL project module
     * @return display name
     */
    public abstract String getDisplayName(Module module);

    /**
     * Gets array of tree node builders.
     * 
     * @return tree node builders
     */
    public abstract TreeNodeBuilder<Object>[] getBuilders();

    /**
     * Gets table view mode.
     * 
     * @return table view mode
     */
    public abstract String getTableMode();

    /**
     * Gets view type.
     * 
     * @return view type
     */
    public abstract Object getType();

    /**
     * Checks that given table syntax node should be shown in view.
     * 
     * @param tableSyntaxNode table syntax node
     * @return <code>true</code> if table syntax node should be shown in view;
     *         <code>false</code> - otherwise
     */
    public abstract boolean select(TableSyntaxNode tableSyntaxNode);
}