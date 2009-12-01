/**
 * Created Jan 25, 2007
 */
package org.openl.rules.ui.view;

import org.openl.base.INamedThing;
import org.openl.base.NamedThing;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.ui.OpenLWrapperInfo;
import org.openl.rules.ui.tree.TreeNodeBuilder;

/**
 * Base class that describes view mode in WebStudio application.
 */
public abstract class WebStudioViewMode extends NamedThing {

    public static final String DEVELOPER_MODE_TYPE = "developer";
    public static final String BUSINESS_MODE_TYPE = "business";

    public static final WebStudioViewMode DEVELOPER_VIEW = new DeveloperViewMode();
    public static final WebStudioViewMode BUSINESS1_VIEW = new BusinessViewMode1();
    public static final WebStudioViewMode BUSINESS2_VIEW = new BusinessViewMode2();
    public static final WebStudioViewMode BUSINESS3_VIEW = new BusinessViewMode3();

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
     * @param wrapper OpenL information wrapper
     * @return display name
     */
    public abstract String getDisplayName(OpenLWrapperInfo wrapper);

    /**
     * Gets folders that used as roots of folder structure.
     * 
     * @return folder names
     */
    public abstract String[][] getFolders();

    /**
     * Gets array of tree node builders.
     * 
     * @return tree node builders
     */
    public abstract TreeNodeBuilder<Object>[][] getBuilders();

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