package org.openl.rules.structure;

import org.openl.rules.table.IGridTable;
import org.openl.util.ISelector;

public abstract class ATableParserElement implements ITableParserElement {

    String name;

    ISelector<IGridTable> selector;

    public String getName() {
        return name;
    }

    public IGridTable parse(IGridTable unparsedTable, ITableObject tobj) {
        if (selector != null && !selector.select(unparsedTable)) {
            return unparsedTable;
        }
        return parseInternal(unparsedTable, tobj);
    }

    protected abstract IGridTable parseInternal(IGridTable unparsedTable, ITableObject tobj);

    public void setName(String name) {
        this.name = name;
    }

}
