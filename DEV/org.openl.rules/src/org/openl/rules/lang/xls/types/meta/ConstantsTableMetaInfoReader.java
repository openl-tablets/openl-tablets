package org.openl.rules.lang.xls.types.meta;

import org.openl.rules.constants.ConstantOpenField;
import org.openl.rules.constants.ConstantsTableBoundNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.types.CellMetaInfo;

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
        var boundNode = getBoundNode();
        var normalizedData = boundNode.getNormalizedData();
        var firstCell = normalizedData.getCell(0, 0);
        var normalOrientation = normalizedData.isNormalOrientation();
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
            var constantName = normalizedData.getCell(1, r).getStringValue();
            for (ConstantOpenField field : boundNode.getConstantOpenFields()) {
                if (field.getName().equals(constantName)) {
                    var type = field.getType();
                    return new CellMetaInfo(type, type.getAggregateInfo().isAggregate(type));
                }
            }
        }

        return null;
    }
}
