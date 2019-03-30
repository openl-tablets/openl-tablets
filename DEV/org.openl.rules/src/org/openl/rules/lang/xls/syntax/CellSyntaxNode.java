package org.openl.rules.lang.xls.syntax;

import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.impl.ASyntaxNode;

public class CellSyntaxNode extends ASyntaxNode {

    public CellSyntaxNode(String type, GridCellSourceCodeModule module) {
        super(type, null, module);
    }

    public GridCellSourceCodeModule getCellSource() {
        return (GridCellSourceCodeModule) getModule();
    }

    @Override
    public ISyntaxNode getChild(int i) {
        throw new IndexOutOfBoundsException("Cell Syntax Node is terminal node");
    }

    @Override
    public int getNumberOfChildren() {
        return 0;
    }

    public String getSourceString() {
        return getCellSource().getCode();
    }

}
