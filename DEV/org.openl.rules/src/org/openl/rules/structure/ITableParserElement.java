package org.openl.rules.structure;

import org.openl.rules.table.ILogicalTable;

public interface ITableParserElement {

    String getName();

    ILogicalTable parse(ILogicalTable unparsedTable, ITableObject tobj);

}
