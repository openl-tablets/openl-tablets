/**
 * Created Feb 27, 2007
 */
package org.openl.rules.table.ui;

import org.openl.rules.table.ICellInfo;
import org.openl.rules.table.IGridRegion;
import org.openl.util.Log;

/**
 * @author snshor
 *
 */
public class FormattedCell  implements ICellInfo
{
	

		ICellInfo delegate;
		
		FormattedCell(ICellInfo delegate)
		{
			this.delegate = delegate;
			font = new CellFont(delegate.getFont());
			style = new CellStyle(delegate.getCellStyle());
		}
		
		public int type;

		public String content;
		
		public Object value;
		
		public CellFont font;

		public CellStyle style;


		public ICellStyle getCellStyle()
		{
			return style;
		}

		public int getColumn()
		{
			return delegate.getColumn();
		}

		public ICellFont getFont()
		{
			return font;
		}

		public int getRow()
		{
			return delegate.getRow();
		}

		public IGridRegion getSurroundingRegion()
		{
			return delegate.getSurroundingRegion();
		}

		public boolean isTopLeft()
		{
			return delegate.isTopLeft();
		}

		public void setFilter(IGridFilter filter)
		{
			if (this.filter != null)
				Log.warn("More than one filter set on cell");
			this.filter = filter;
		}
		
		IGridFilter filter;

		public IGridFilter getFilter()
		{
			return filter;
		}
	

}
