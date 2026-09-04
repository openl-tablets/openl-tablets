package org.openl.rules.structure;

import lombok.Getter;
import lombok.Setter;

import org.openl.rules.table.ILogicalTable;

public abstract class ATableParserElement implements ITableParserElement {

    @Getter
    @Setter
    String name;

    @Override
    public ILogicalTable parse(ILogicalTable unparsedTable, ITableObject tobj) {
        return parseInternal(unparsedTable, tobj);
    }

    protected abstract ILogicalTable parseInternal(ILogicalTable unparsedTable, ITableObject tobj);

}
