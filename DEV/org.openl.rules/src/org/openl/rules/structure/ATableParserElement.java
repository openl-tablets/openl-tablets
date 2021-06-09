package org.openl.rules.structure;

import org.openl.rules.table.ILogicalTable;

public abstract class ATableParserElement implements ITableParserElement {

    String name;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public ILogicalTable parse(ILogicalTable unparsedTable, ITableObject tobj) {
        return parseInternal(unparsedTable, tobj);
    }

    protected abstract ILogicalTable parseInternal(ILogicalTable unparsedTable, ITableObject tobj);

    public void setName(String name) {
        this.name = name;
    }

}
