package org.openl.tablets.tutorial6.sudoku;

import org.openl.rules.dt.DecisionTable;
import org.openl.rules.dt.IDecisionTableConstants;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.GridTable;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ui.FilteredGrid;
import org.openl.rules.table.ui.IGridFilter;
import org.openl.rules.table.ui.SimpleHtmlFilter;
import org.openl.rules.table.ui.TableValueFilter;
import org.openl.rules.table.xls.SimpleXlsFormatter;
import org.openl.types.IOpenMethod;
import org.openl.types.impl.AOpenClass;
import org.openl.types.impl.DynamicObject;

public class GameInterface
{

    public IGridTable display(String methodName, Object thizz, final int[][] res)
    {
	
	
	IOpenMethod m = AOpenClass.getSingleMethod(methodName, ((DynamicObject)thizz).getType().methods());
	DecisionTable dt = (DecisionTable)m;
	TableSyntaxNode tsn =  dt.getTableSyntaxNode();
	
	IGridTable gt = (IGridTable)tsn.getSubTables().get(IDecisionTableConstants.VIEW_BUSINESS);
	
	TableValueFilter.Model model = new TableValueFilter.Model()
	{

	    public Object getValue(int col, int row)
	    {
		if (row <= 0) return null;
		if (col < 0) return null;
		if (res.length <= col || res.length < row)
		    return null;
		
//		System.out.println(col + ":" + row + ":" + res[row-1][col]);
		return res[row-1][col];
	    }
	    
	};
	
	IGridFilter[] filters = 
	{
		new TableValueFilter(gt, model),
		new SimpleXlsFormatter(),
		new SimpleHtmlFilter(),
	};
	
	FilteredGrid fg = new FilteredGrid(gt.getGrid(), filters);
	
	return new GridTable(gt.getRegion(), fg);
    }
    
}
