package org.openl.rules.lang.xls.types.meta;

import org.openl.rules.datatype.binding.AliasDatatypeBoundNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.types.CellMetaInfo;
import org.openl.types.IOpenClass;

public class AliasDatatypeMetaInfoReader extends BaseMetaInfoReader<AliasDatatypeBoundNode> {
    public AliasDatatypeMetaInfoReader(AliasDatatypeBoundNode boundNode) {
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
    public CellMetaInfo getBodyMetaInfo(int row, int col) {
        IOpenClass baseClass = getBoundNode().getDomainOpenClass().getBaseClass();
        boolean isArray = baseClass.isArray();
        if (isArray) {
            baseClass = baseClass.getAggregateInfo().getComponentType(baseClass);
        }
        return new CellMetaInfo(baseClass, isArray);
    }
}
