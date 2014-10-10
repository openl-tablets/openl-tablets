package org.openl.binding.impl;

import org.openl.rules.data.ITable;
import org.openl.util.text.ILocation;
import org.openl.util.text.TextInfo;

/**
 * @author nsamatov.
 */
public class TableUsage extends SimpleNodeUsage {
    public TableUsage(ITable foreignTable, int start, int end, NodeType nodeType) {
        super(start, end, foreignTable.getTableSyntaxNode().getHeaderLineValue().getValue(),
                foreignTable.getTableSyntaxNode().getUri(), nodeType);
    }

    public TableUsage(ITable foreignTable, ILocation location, NodeType nodeType) {
        super(location.getStart().getAbsolutePosition(new TextInfo(foreignTable.getName())),
                location.getEnd().getAbsolutePosition(new TextInfo(foreignTable.getName())) - 1,
                foreignTable.getTableSyntaxNode().getHeaderLineValue().getValue(),
                foreignTable.getTableSyntaxNode().getUri(), nodeType);
    }

}
