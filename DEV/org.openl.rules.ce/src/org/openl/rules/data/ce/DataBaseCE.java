package org.openl.rules.data.ce;

import org.openl.rules.data.DataBase;
import org.openl.rules.data.ITable;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;

public class DataBaseCE extends DataBase {

	@Override
	protected ITable makeNewTable(String tableName, TableSyntaxNode tsn) {
		return new TableCE(tableName, tsn);
	}

}
