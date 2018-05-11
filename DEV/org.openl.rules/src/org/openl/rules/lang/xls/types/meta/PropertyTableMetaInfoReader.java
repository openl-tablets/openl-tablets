package org.openl.rules.lang.xls.types.meta;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.types.CellMetaInfo;
import org.openl.rules.property.PropertyTableBoundNode;

public class PropertyTableMetaInfoReader extends BaseMetaInfoReader<PropertyTableBoundNode> {
    public PropertyTableMetaInfoReader(PropertyTableBoundNode boundNode) {
        super(boundNode);
    }

    @Override
    protected TableSyntaxNode getTableSyntaxNode() {
        return getBoundNode().getTableSyntaxNode();
    }

    @Override
    protected CellMetaInfo getHeaderMetaInfo() {
        return null;
    }

    @Override
    protected CellMetaInfo getBodyMetaInfo(int row, int col) {
        return null;
    }
}
