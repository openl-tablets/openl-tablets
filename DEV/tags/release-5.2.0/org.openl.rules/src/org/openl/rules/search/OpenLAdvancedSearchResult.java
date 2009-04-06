/**
 * Created May 4, 2007
 */
package org.openl.rules.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;


/**
 * @author snshor
 *
 */
public class OpenLAdvancedSearchResult
{

	
	OpenLAdvancedSearch search;

	
	ArrayList<TableAndRows> foundTables = new ArrayList<TableAndRows>();
	

	public OpenLAdvancedSearchResult(OpenLAdvancedSearch search)
	{
		this.search = search;
	}


	static public class TableAndRows
	{
		TableSyntaxNode tsn;
		ISearchTableRow[] rows;
		public TableAndRows(TableSyntaxNode tsn, ISearchTableRow[] rows)
		{
			this.rows = rows;
			this.tsn = tsn;
		}
		public ISearchTableRow[] getRows()
		{
			return this.rows;
		}
		public TableSyntaxNode getTsn()
		{
			return this.tsn;
		}
	}


	/**
	 * @param tsn
	 * @param rows
	 */
	public void add(TableSyntaxNode tsn, ISearchTableRow[] rows)
	{
		foundTables.add(new TableAndRows(tsn, rows));
	}


	/**
	 * @return
	 */
	public TableAndRows[] tablesAndRows()
	{
		TableAndRows[] tr = (TableAndRows[])foundTables.toArray(new TableAndRows[0]);
		
		Arrays.sort(tr, new Comparator<TableAndRows>(){

			public int compare(TableAndRows t1, TableAndRows t2)
			{
				return t2.rows.length - t1.rows.length;
			}});
		return tr;
	}
	
	
	
}
