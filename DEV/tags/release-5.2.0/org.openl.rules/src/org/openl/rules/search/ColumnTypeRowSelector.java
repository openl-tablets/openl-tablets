/**
 * Created Apr 19, 2007
 */
package org.openl.rules.search;

import org.openl.types.IOpenClass;
import org.openl.util.AStringBoolOperator;

/**
 * @author snshor
 * 
 */

public class ColumnTypeRowSelector extends ATableCellValueSelector
{
	AStringBoolOperator columnTypeSelector;


	/**
	 * @param se
	 */
	public ColumnTypeRowSelector(SearchElement se)
	{
		columnTypeSelector = se.isAny(se.getValue1()) ? null : AStringBoolOperator
				.makeOperator(se.getOpType1(), se.getValue1());
		cellValueSelector = se.isAny(se.getValue2()) ? null : AStringBoolOperator
				.makeOperator(se.getOpType2(), se.getValue2());
	}



	public boolean selectRowInTable(ISearchTableRow row, ITableSearchInfo tsi)
	{
		int nc = tsi.numberOfColumns();

			for (int c = 0; c < nc; c++)
			{
				if (columnTypeSelector != null)
				{
					IOpenClass ctype = tsi.columnType(c);
					String ctypeName = ctype.getName();
					if (!columnTypeSelector.op(ctypeName))
						continue;
				}
				
				if (cellValueSelector == null)
					return true;

				Object cellValue = tsi.tableValue(c, row.getRow());
				if (selectCellValue(cellValue))
					return true;
				
			}

		return false;
	}
	
	

}
