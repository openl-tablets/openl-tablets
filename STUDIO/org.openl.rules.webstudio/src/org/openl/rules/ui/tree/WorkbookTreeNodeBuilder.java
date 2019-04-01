package org.openl.rules.ui.tree;

import java.util.LinkedHashMap;

import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.ui.IProjectTypes;

/**
 * Builds tree node for workbook.
 */
public class WorkbookTreeNodeBuilder extends BaseTableTreeNodeBuilder {

    private static final String WORKBOOK_NAME = "workbook";

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] getDisplayValue(Object nodeObject, int i) {

        XlsWorkbookSourceCodeModule wb = (XlsWorkbookSourceCodeModule) nodeObject;

        return new String[] { wb.getDisplayName(), wb.getUri(), wb.getUri() };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return WORKBOOK_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getType(Object sorterObject) {
        return IProjectTypes.PT_WORKBOOK;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getUrl(Object nodeObject) {

        XlsWorkbookSourceCodeModule workbook = (XlsWorkbookSourceCodeModule) nodeObject;

        return workbook.getUri();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getWeight(Object sorterObject) {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object makeObject(TableSyntaxNode tsn) {
        return tsn.getXlsSheetSourceCodeModule().getWorkbookSource();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getProblems(Object nodeObject) {
        return null;
    }

    @Override
    public ITreeNode<Object> makeNode(TableSyntaxNode tableSyntaxNode, int i) {
        TreeNode<Object> treeNode = (TreeNode<Object>) super.makeNode(tableSyntaxNode, i);
        // Put spreadsheets in order as they defined in xls, not sort them alphabetically
        treeNode.setElements(new LinkedHashMap<>(treeNode.getElements()));
        return treeNode;
    }
}
