/**
 * Created Feb 27, 2007
 */
package org.openl.rules.table;

/**
 * @author snshor
 *
 */
public class CellKey
{
		int col, row;

		public CellKey(int col, int row)
		{
			this.col = col;
			this.row = row;
		}

		public boolean equals(Object obj)
		{
			CellKey k = (CellKey) obj;
			return col == k.col && row == k.row;
		}

		public int hashCode()
		{
			return col * 37 + row;
		}


}
