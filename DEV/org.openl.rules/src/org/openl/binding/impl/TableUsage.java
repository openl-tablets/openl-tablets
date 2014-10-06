package org.openl.binding.impl;

import org.openl.rules.data.ITable;
import org.openl.util.text.ILocation;
import org.openl.util.text.TextInfo;

/**
 * @author nsamatov.
 */
public class TableUsage implements NodeUsage {
    private final ITable foreignTable;
    private final ILocation location;
    private final NodeType nodeType;

    public TableUsage(ITable foreignTable, ILocation location, NodeType nodeType) {
        this.foreignTable = foreignTable;
        this.location = location;
        this.nodeType = nodeType;
    }

    @Override
    public int getStart() {
        return location.getStart().getAbsolutePosition(new TextInfo(foreignTable.getName()));
    }

    @Override
    public int getEnd() {
        return location.getEnd().getAbsolutePosition(new TextInfo(foreignTable.getName())) - 1;
    }

    @Override
    public String getDescription() {
        return foreignTable.getTableSyntaxNode().getHeaderLineValue().getValue();
    }

    @Override
    public String getUri() {
        return foreignTable.getTableSyntaxNode().getUri();
    }

    @Override
    public NodeType getNodeType() {
        return nodeType;
    }
}
