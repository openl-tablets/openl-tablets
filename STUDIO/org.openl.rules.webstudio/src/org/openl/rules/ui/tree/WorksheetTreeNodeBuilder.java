package org.openl.rules.ui.tree;

import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.ui.IProjectTypes;

/**
 * Builds tree node for work sheet.
 */
public class WorksheetTreeNodeBuilder extends BaseTableTreeNodeBuilder {

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] getDisplayValue(Object nodeObject, int i) {

        XlsSheetSourceCodeModule sheet = (XlsSheetSourceCodeModule) nodeObject;

        return new String[] { sheet.getSheetName(), sheet.getSheetName(), sheet.getSheetName() };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getType(Object nodeObject) {
        return IProjectTypes.PT_WORKSHEET;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getUrl(Object nodeObject) {

        XlsSheetSourceCodeModule sheet = (XlsSheetSourceCodeModule) nodeObject;

        return sheet.getUri();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object makeObject(TableSyntaxNode tableSyntaxNode) {
        return tableSyntaxNode.getModule();
    }
}
