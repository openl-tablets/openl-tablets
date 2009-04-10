/**
 * Created Apr 29, 2007
 */
package org.openl.rules.search;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.util.ArrayTool;

/**
 * @author snshor
 *
 */
public class TableTypeSelector extends ATableSyntaxNodeSelector
{
	String[] types;
	
	public TableTypeSelector()
	{
	}
	
	
	public TableTypeSelector(String[] types)
	{
		this.types = types;
	}

	public boolean selectTable(TableSyntaxNode node)
	{
		String type = node.getType();
		return ArrayTool.contains(types, type);
	}

	public String[] getTypes()
	{
		return this.types;
	}

	public void setTypes(String[] types)
	{
		this.types = types;
	}

}
