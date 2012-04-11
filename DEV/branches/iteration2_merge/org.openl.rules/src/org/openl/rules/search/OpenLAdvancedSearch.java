/**
 * Created May 3, 2007
 */
package org.openl.rules.search;

import java.util.ArrayList;

import org.openl.rules.lang.xls.ITableNodeTypes;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.XlsModuleSyntaxNode;
import org.openl.syntax.ISyntaxError;
import org.openl.util.AStringBoolOperator;
import org.openl.util.ArrayTool;

/**
 * @author snshor
 * 
 */
public class OpenLAdvancedSearch implements ITableNodeTypes, ISearchConstants
{

	static public String[] typeButtons = { "Rules", "Spreadsheet", "TBasic", "Column Match", "Data",
	    "Method", "Datatype", "Test", "Run", "Env", "Other" };

	static public String[] types = { XLS_DT, XLS_SPREADSHEET, XLS_TBASIC, XLS_COLUMN_MATCH, XLS_DATA,
	    XLS_METHOD, XLS_DATATYPE, XLS_TEST_METHOD, XLS_RUN_METHOD, XLS_ENVIRONMENT, XLS_OTHER };

	boolean[] selectedType = new boolean[typeButtons.length];
	
	static public String[] nfValues = {"", "NOT"};

	static public final boolean[] typeNeedValue1 = {false, true};
	
	
	SearchElement[] tableElements = {new SearchElement(HEADER), new SearchElement(PROPERTY)};
	SearchElement[] columnElements = {new SearchElement(COLUMN_NAME), new SearchElement(COLUMN_TYPE)};
	
	public String[] getGopValues()
	{
		return GroupOperator.names;
	}
	
	public void selectType(int i, boolean x)
	{
		selectedType[i] = x;
	}

	public boolean selectType(int i)
	{
		return selectedType[i];
	}

	public String[] getTypeButtons()
	{
		return typeButtons;
	}

	ATableRowSelector[] rowSelectors()
	{
		ArrayList<ATableRowSelector> list = new ArrayList<ATableRowSelector>();
		
		list.add(new ColumnGroupSelector(columnElements));

		return list.toArray(new ATableRowSelector[0]);

	}

	ATableSyntaxNodeSelector[] tableSelectors()
	{
		ArrayList<ATableSyntaxNodeSelector> sll = new ArrayList<ATableSyntaxNodeSelector>();

		sll.add(makeTableTypeSelector());

		sll.add(makePropertyOrHeaderSelectors());
		
		
		return  sll
				.toArray(new ATableSyntaxNodeSelector[0]);
	}

	/**
	 * @return
	 */
	private ATableSyntaxNodeSelector makePropertyOrHeaderSelectors()
	{
		return new TableGroupSelector(tableElements);
	}

	TableTypeSelector makeTableTypeSelector()
	{
		ArrayList<String> list = new ArrayList<String>();
		for (int i = 0; i < selectedType.length; i++)
		{
			if (selectedType[i])
				list.add(types[i]);
		}

		String[] tt = (String[]) list.toArray(new String[0]);

		return new TableTypeSelector(tt);
	}

	/**
	 * @param xsn
	 * @return
	 */
	public Object search(XlsModuleSyntaxNode xsn)
	{
		ATableSyntaxNodeSelector[] tselectors = tableSelectors();
		ATableRowSelector[] rselectors = rowSelectors();

//		ArrayList tableList = new ArrayList();
//		ArrayList rowList = new ArrayList();

		OpenLAdvancedSearchResult res = new OpenLAdvancedSearchResult(this);
		
		TableSyntaxNode[] tsnn = xsn.getXlsTableSyntaxNodesWithoutErrors();
		for (int i = 0; i < tsnn.length; i++)
		{
			TableSyntaxNode tsn = tsnn[i];

			ISyntaxError[] errors = tsn.getErrors();
			if (errors != null && errors.length > 0) {
			    continue;
			}

			if (!isTableSelected(tsn, tselectors))
				continue;

			ITableSearchInfo tsi = ATableRowSelector.getTableSearchInfo(tsn);
			if (tsi == null)
			{
				res.add(tsn, new ISearchTableRow[0]);
				continue;
			}

			ArrayList<ISearchTableRow> selectedRows = new ArrayList<ISearchTableRow>();
			int nrows = tsi.numberOfRows();
			for (int row = 0; row < nrows; row++) {
				ISearchTableRow tr = new SearchTableRow(row, tsi);
				if (!(tsi instanceof TableSearchInfo)
				        && !isRowSelected(tr, rselectors, tsi)) {
					continue;
				}
				selectedRows.add(tr);
			}
			
			res.add(tsn, selectedRows.toArray(new ISearchTableRow[0]));
		}

		return res;
	}

	boolean isTableSelected(TableSyntaxNode tsn,
			ATableSyntaxNodeSelector[] tselectors)
	{
		for (int j = 0; j < tselectors.length; j++)
		{
			if (!tselectors[j].selectTable(tsn))
				return false;
		}

		return true;

	}

	boolean isRowSelected(ISearchTableRow trow, ATableRowSelector[] rselectors,
			ITableSearchInfo tsi)
	{
		for (int j = 0; j < rselectors.length; j++)
		{
			if (!rselectors[j].selectRowInTable(trow, tsi))
				return false;
		}

		return true;

	}

	public SearchElement[] getTableElements()
	{
		return this.tableElements;
	}

	public void setTableElements(SearchElement[] tableElements)
	{
		this.tableElements = tableElements;
	}
	
	public boolean showValue1(String typeValue)
	{
		for (int i = 0; i < typeValues.length; i++)
		{
			if (typeValues[i].equals(typeValue))
				return typeNeedValue1[i];
		}
		throw new RuntimeException("Unknown type value: " + typeValue);
	}

	public String[] opTypeValues()
	{
		return AStringBoolOperator.allNames();
	}

	public void fillTableElement(int i, String gopID, String nfID, String typeID, String value1ID, String opTypeID, String value2ID)
	{
		if (i >= tableElements.length)
			return;
		
	if (typeID == null)
		typeID = PROPERTY;
	
		SearchElement se = new SearchElement(typeID);
		
		se.setOperator(GroupOperator.find(gopID));
		se.setNotFlag(nfValues[1].equals(nfID));
		if (value1ID != null)
			se.setValue1(value1ID);
		se.setOpType2(opTypeID);
		se.setValue2(value2ID);
		
		tableElements[i] = se;
		
		
	}

	public void fillColumnElement(int i, String gopID, String nfID, String typeID, String opType1ID, String value1ID, 
			String opType2ID, String value2ID)
	{
		if (i >= columnElements.length)
			return;
		
	if (typeID == null)
		typeID = COLUMN_NAME;
	
		SearchElement se = new SearchElement(typeID);
		
		se.setOperator(GroupOperator.find(gopID));
		se.setNotFlag(nfValues[1].equals(nfID));
		
		se.setOpType1(opType1ID);
		if (value1ID != null)
			se.setValue1(value1ID);
		se.setOpType2(opType2ID);
		se.setValue2(value2ID);
		
		columnElements[i] = se;
		
		
	}
	
	
	
	
/*	
	public void fillTableElement(int i, String gopID, String nfID, String typeID, String value1ID, String opTypeID, String value2ID)
	{
		boolean isEmpty = typeID == null;
		int MIN_LEN = 1;
		
		if (isEmpty)
		{	
			if (i < tableElements.length)
			  makeLengthTableElements(Math.max(i, MIN_LEN));
			return;
		}	
		makeLengthTableElements(Math.max(i+1, MIN_LEN));
	
		
		
		SearchElement se = new SearchElement(typeID);
		
		se.setOperator(GroupOperator.find(gopID));
		se.setNotFlag(nfValues[1].equals(nfID));
		se.setValue1(value1ID);
		se.setOpType(opTypeID);
		se.setValue2(value2ID);
		
		tableElements[i] = se;
		
		
	}
	
*/	

	/**
	 * @param tableElements2
	 * @param i
	 */
//	private void makeLengthTableElements(int len)
//	{
//		SearchElement[] xx = new SearchElement[len];
//		System.arraycopy(tableElements, 0, xx, 0, Math.min(len, tableElements.length));
//		tableElements = xx;
//	}
	
	
	public void editAction(String action)
	{
		if (action.startsWith(ADD_ACTION))
		{
			addTablePropertyAfter(Integer.parseInt(action.substring(ADD_ACTION.length())));
		}	
		else if (action.startsWith(DELETE_ACTION))
		{
			deleteTablePropertyAt(Integer.parseInt(action.substring(DELETE_ACTION.length())));
		}
		else if (action.startsWith(COL_ADD_ACTION))
		{
			addColumnPropertyAfter(Integer.parseInt(action.substring(COL_ADD_ACTION.length())));
		}	
		else if (action.startsWith(COL_DELETE_ACTION))
		{
			deleteColumnPropertyAt(Integer.parseInt(action.substring(COL_DELETE_ACTION.length())));
		}	
		
	}

	/**
	 * @param i
	 */
	private void deleteTablePropertyAt(int i)
	{
		tableElements = (SearchElement[])ArrayTool.removeValue(i, tableElements);
	}

	private void deleteColumnPropertyAt(int i)
	{
		columnElements = (SearchElement[])ArrayTool.removeValue(i, columnElements);
	}

	
	/**
	 * @param i
	 */
	private void addTablePropertyAfter(int i)
	{
		tableElements = (SearchElement[])ArrayTool.insertValue(i+1, tableElements, tableElements[i].copy());
	}

	private void addColumnPropertyAfter(int i)
	{
		columnElements = (SearchElement[])ArrayTool.insertValue(i+1, columnElements, columnElements[i].copy());
	}
	
	
	
	public SearchElement[] getColumnElements()
	{
		return this.columnElements;
	}

	public void setColumnElements(SearchElement[] columnElements)
	{
		this.columnElements = columnElements;
	}
	
}
