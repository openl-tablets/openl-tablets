package org.openl.rules.lang.xls.types.meta;

import org.openl.rules.constants.ConstantOpenField;
import org.openl.rules.constants.ConstantsTableBoundNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.types.CellMetaInfo;
import org.openl.rules.table.ICell;
import org.openl.rules.table.ILogicalTable;
import org.openl.types.IOpenClass;

public class ConstantsTableMetaInfoReader extends BaseMetaInfoReader<ConstantsTableBoundNode> {
    public ConstantsTableMetaInfoReader(ConstantsTableBoundNode boundNode) {
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
        ConstantsTableBoundNode boundNode = getBoundNode();
        ILogicalTable normalizedData = boundNode.getNormalizedData();
        ICell firstCell = normalizedData.getCell(0, 0);
        boolean normalOrientation = normalizedData.isNormalOrientation();
        int c;
        int r;
        if (normalOrientation) {
            r = row - firstCell.getAbsoluteRow();
            c = col - firstCell.getAbsoluteColumn();
        } else {
            r = col - firstCell.getAbsoluteColumn();
            c = row - firstCell.getAbsoluteRow();
        }

        if (c == 2 && r >= 0) {
            // Constant value column
            String constantName = normalizedData.getCell(1, r).getStringValue();
            for (ConstantOpenField field : boundNode.getConstantOpenFields()) {
                if (field.getName().equals(constantName)) {
                    IOpenClass type = field.getType();
                    return new CellMetaInfo(type, type.getAggregateInfo().isAggregate(type));
                }
            }
        }

        return null;
    }
}
