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

    public TableUsage(ITable foreignTable, ILocation location) {
        this.foreignTable = foreignTable;
        this.location = location;
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
}
