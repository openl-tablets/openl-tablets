/**
 * Created Apr 29, 2007
 */
package org.openl.rules.search;

import java.util.ArrayList;

import org.openl.rules.dt.DTParameterInfo;
import org.openl.rules.dt.DecisionTable;
import org.openl.rules.dt.IDTAction;
import org.openl.rules.dt.IDTCondition;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.IGridTable;
import org.openl.types.IOpenClass;

/**
 * @author snshor
 *
 */
public class DTTableSearchInfo implements ITableSearchInfo
{

	TableSyntaxNode tsn;
	DecisionTable dt;
	DTParameterInfo[] params;
	
	public DTTableSearchInfo(TableSyntaxNode tsn)
	{
		this.tsn = tsn;
		dt = (DecisionTable)tsn.getMember();
	}
	
	public int numberOfRows()
	{
		return dt.getNumberOfRules();
	}
	
	
	public DTParameterInfo[] getParams()
	{
		if (params == null)
		{
			ArrayList<DTParameterInfo> list = new ArrayList<DTParameterInfo>(20);
			for (int i = 0; i < dt.getConditionRows().length; i++)
			{
				IDTCondition c = dt.getConditionRows()[i];
				int n = c.numberOfParams();
				for (int j = 0; j < n; j++)
				{
					list.add(c.getParameterInfo(j));
				}
				
			}
			
			for (int i = 0; i < dt.getActionRows().length; i++)
			{
				IDTAction a = dt.getActionRows()[i];
				int n = a.numberOfParams();
				for (int j = 0; j < n; j++)
				{
					list.add(a.getParameterInfo(j));
				}
			}
			
			params = list.toArray(new DTParameterInfo[0]);
			
		}
		return params;
	}

	public int numberOfColumns()
	{
		return getParams().length;
	}

	public String columnName(int n)
	{
		return getParams()[n].getParameterDeclaration().getName();
	}

	public String columnDisplay(int n)
	{
		return getParams()[n].getPresentation();
	}

	public IOpenClass columnType(int n)
	{
		return getParams()[n].getParameterDeclaration().getType();
	}

	public Object tableValue(int col, int row)
	{
		return getParams()[col].getValue(row);
	}

	public IGridTable rowTable(int row)
	{
		return dt.getRuleTable(row).getGridTable();
	}

	public IGridTable headerDisplayTable()
	{
		return dt.getDisplayTable().getGridTable();
	}

	public TableSyntaxNode getTableSyntaxNode()
	{
		return tsn;
	}

}
