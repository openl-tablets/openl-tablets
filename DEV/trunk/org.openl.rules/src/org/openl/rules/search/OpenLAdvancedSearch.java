/**
 * Created May 3, 2007
 */
package org.openl.rules.search;

import java.util.ArrayList;

import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.XlsModuleSyntaxNode;
import org.openl.util.AStringBoolOperator;
import org.openl.util.ArrayTool;

/**
 * @author snshor
 *
 *
 */
public class OpenLAdvancedSearch implements ISearchConstants, IOpenLSearch {
    
    /*
     * Type of components where we search the results. 
     */
    public static final String[] EXISTING_TABLE_TYPES = { "Rules", "Spreadsheet", "TBasic", "Column Match", "Data",
        "Method", "Datatype", "Test", "Run", "Env", "Other" };

    public static final String[] TYPES = { XlsNodeTypes.XLS_DT.toString(), XlsNodeTypes.XLS_SPREADSHEET.toString(), 
        XlsNodeTypes.XLS_TBASIC.toString(), XlsNodeTypes.XLS_COLUMN_MATCH.toString(), XlsNodeTypes.XLS_DATA.toString(), 
        XlsNodeTypes.XLS_METHOD.toString(), XlsNodeTypes.XLS_DATATYPE.toString(), 
        XlsNodeTypes.XLS_TEST_METHOD.toString(), XlsNodeTypes.XLS_RUN_METHOD.toString(), 
        XlsNodeTypes.XLS_ENVIRONMENT.toString(), XlsNodeTypes.XLS_OTHER.toString() };

    public static final  String[] NF_VALUES = { "", "NOT" };

    public static final boolean[] TYPE_NEED_VALUE1 = { false, true };

    private boolean[] selectedTableTypes = new boolean[EXISTING_TABLE_TYPES.length];

    private SearchConditionElement[] tableElements = { new SearchConditionElement(HEADER)};
    private SearchConditionElement[] columnElements = { new SearchConditionElement(COLUMN_PARAMETER)};

    private void addColumnPropertyAfter(int i) {
        columnElements = (SearchConditionElement[]) ArrayTool.insertValue(i + 1, columnElements, columnElements[i].copy());
    }

    /**
     * @param i
     */
    private void addTablePropertyAfter(int i) {
        tableElements = (SearchConditionElement[]) ArrayTool.insertValue(i + 1, tableElements, tableElements[i].copy());
    }

    private void deleteColumnPropertyAt(int i) {
        columnElements = (SearchConditionElement[]) ArrayTool.removeValue(i, columnElements);
    }

    /**
     * @param i
     */
    private void deleteTablePropertyAt(int i) {
        tableElements = (SearchConditionElement[]) ArrayTool.removeValue(i, tableElements);
    }
    
    private boolean isRowSelected(ISearchTableRow searchTableRow, ATableRowSelector[] rowSelectors,
            ITableSearchInfo tableSearchInfo) {
        for (int j = 0; j < rowSelectors.length; j++) {
            if (!rowSelectors[j].isRowInTableSelected(searchTableRow, tableSearchInfo)) {
                return false;
            }
        }

        return true;

    }
    
    /** 
     * @param tsn <code>TableSyntaxNode</code> representing the table.
     * @param tselectors Selectors that were defined on advanced search.
     * @return <code>True</code> if table matches to all the selectors. If it does not match even to the one selector
     * returns <code>false</code>.
     */
    private boolean doesTableMatcheToSelectors(TableSyntaxNode tsn, ATableSyntaxNodeSelector[] tselectors) {
        for (int j = 0; j < tselectors.length; j++) {
            if (!tselectors[j].doesTableMatch(tsn)) {
                return false;
            }
        }
        return true;
    }

    /**
     * @return
     */
    private ATableSyntaxNodeSelector makePropertyOrHeaderSelectors() {
        return new TableGroupSelector(tableElements);
    }

    private TableTypeSelector makeTableTypeSelector() {
        ArrayList<String> list = new ArrayList<String>();
        for (int i = 0; i < selectedTableTypes.length; i++) {
            if (selectedTableTypes[i]) {
                list.add(TYPES[i]);
            }
        }

        String[] tt = list.toArray(new String[0]);

        return new TableTypeSelector(tt);
    }
    
    private void addResult(TableSyntaxNode table, OpenLAdvancedSearchResult res, 
            ATableSyntaxNodeSelector[] tableSelectors, ATableRowSelector[] columnSelectors) {        
        if (doesTableMatcheToSelectors(table, tableSelectors)) {
            ITableSearchInfo tablSearchInfo = ATableRowSelector.getTableSearchInfo(table);
            if (tablSearchInfo != null) {
                ArrayList<ISearchTableRow> matchedRows = getMatchedRows(tablSearchInfo, columnSelectors);
                res.add(table, matchedRows.toArray(new ISearchTableRow[0]));
            } else {
                res.add(table, new ISearchTableRow[0]);
            }
        }
    }
    
    /**
     * Gets the rows of tables that matches to the search info.
     * @param tablSearchInfo
     * @param columnSelectors
     * @return
     */
    private ArrayList<ISearchTableRow> getMatchedRows(ITableSearchInfo tablSearchInfo, ATableRowSelector[] columnSelectors) {
        ArrayList<ISearchTableRow> matchedRows = new ArrayList<ISearchTableRow>();
        int numRows = tablSearchInfo.getNumberOfRows();
        for (int row = 0; row < numRows; row++) {
            ISearchTableRow tablSearchRow = new SearchTableRow(row, tablSearchInfo);
            // Now there is implementations for ITableSearchInfo just for data tables (DataTableSearchInfo class) and 
            // for decision tables (DecisionTableSearchInfo class) all other tables process as TableSearchInfo. It must 
            // be implemented for other types.
            if (!(tablSearchInfo instanceof TableSearchInfo) && !isRowSelected(tablSearchRow, columnSelectors, tablSearchInfo)) {
                continue;
            }
            matchedRows.add(tablSearchRow);
        }
        return matchedRows;
    }
    
    private ATableSyntaxNodeSelector[] getTableSelectors() {
        ArrayList<ATableSyntaxNodeSelector> sll = new ArrayList<ATableSyntaxNodeSelector>();

        sll.add(makeTableTypeSelector());

        sll.add(makePropertyOrHeaderSelectors());

        return sll.toArray(new ATableSyntaxNodeSelector[0]);
    }

    /**
     * @param tableElements2
     * @param i
     */
    // private void makeLengthTableElements(int len)
    // {
    // SearchElement[] xx = new SearchElement[len];
    // System.arraycopy(tableElements, 0, xx, 0, Math.min(len,
    // tableElements.length));
    // tableElements = xx;
    // }

    public void editAction(String action) {
        if (action.startsWith(ADD_ACTION)) {
            addTablePropertyAfter(Integer.parseInt(action.substring(ADD_ACTION.length())));
        } else if (action.startsWith(DELETE_ACTION)) {
            deleteTablePropertyAt(Integer.parseInt(action.substring(DELETE_ACTION.length())));
        } else if (action.startsWith(COL_ADD_ACTION)) {
            addColumnPropertyAfter(Integer.parseInt(action.substring(COL_ADD_ACTION.length())));
        } else if (action.startsWith(COL_DELETE_ACTION)) {
            deleteColumnPropertyAt(Integer.parseInt(action.substring(COL_DELETE_ACTION.length())));
        }

    }
    
    @Deprecated
    public void fillColumnElement(int i, String gopID, String nfID, String typeID, String opType1ID, String value1ID,
            String opType2ID, String value2ID) {
        if (i >= columnElements.length) {
            return;
        }

        if (typeID == null) {
            typeID = COLUMN_PARAMETER;
        }

        SearchConditionElement se = new SearchConditionElement(typeID);

        se.setGroupOperator(GroupOperator.find(gopID));
        se.setNotFlag(NF_VALUES[1].equals(nfID));

        se.setOpType1(opType1ID);
        if (value1ID != null) {
            se.setElementValueName(value1ID);
        }
        se.setOpType2(opType2ID);
        se.setElementValue(value2ID);

        columnElements[i] = se;

    }
    
    @Deprecated
    public void fillTableElement(int i, String gopID, String nfID, String typeID, String value1ID, String opTypeID,
            String value2ID) {
        if (i >= tableElements.length) {
            return;
        }

        if (typeID == null) {
            typeID = PROPERTY;
        }

        SearchConditionElement se = new SearchConditionElement(typeID);

        se.setGroupOperator(GroupOperator.find(gopID));
        se.setNotFlag(NF_VALUES[1].equals(nfID));
        if (value1ID != null) {
            se.setElementValueName(value1ID);
        }
        se.setOpType2(opTypeID);
        se.setElementValue(value2ID);

        tableElements[i] = se;

    }

    public SearchConditionElement[] getColumnElements() {
        return columnElements;
    }

    public String[] getGroupOperatorNames() {
        return GroupOperator.names;
    }

    public SearchConditionElement[] getTableElements() {
        return tableElements;
    }

    public String[] getExistingTableTypes() {
        return EXISTING_TABLE_TYPES;
    }

    

    public String[] opTypeValues() {
        return AStringBoolOperator.getAllOperatorNames();
    }

    public ATableRowSelector[] getColumnSelectors() {
        ArrayList<ATableRowSelector> list = new ArrayList<ATableRowSelector>();

        list.add(new ColumnGroupSelector(columnElements));

        return list.toArray(new ATableRowSelector[0]);

    }

    /*
     * public void fillTableElement(int i, String gopID, String nfID, String
     * typeID, String value1ID, String opTypeID, String value2ID) { boolean
     * isEmpty = typeID == null; int MIN_LEN = 1;
     *
     * if (isEmpty) { if (i < tableElements.length)
     * makeLengthTableElements(Math.max(i, MIN_LEN)); return; }
     * makeLengthTableElements(Math.max(i+1, MIN_LEN));
     *
     *
     *
     * SearchElement se = new SearchElement(typeID);
     *
     * se.setOperator(GroupOperator.find(gopID));
     * se.setNotFlag(nfValues[1].equals(nfID)); se.setValue1(value1ID);
     * se.setOpType(opTypeID); se.setValue2(value2ID);
     *
     * tableElements[i] = se;
     *
     *  }
     *
     */

    public Object search(XlsModuleSyntaxNode xsn) {
        ATableSyntaxNodeSelector[] tableSelectors = getTableSelectors();
        ATableRowSelector[] columnSelectors = getColumnSelectors();

        OpenLAdvancedSearchResult res = new OpenLAdvancedSearchResult();

        TableSyntaxNode[] tables = xsn.getXlsTableSyntaxNodesWithoutErrors();
        for (TableSyntaxNode table : tables) {
            if (!table.hasErrors()) {
                addResult(table, res, tableSelectors, columnSelectors);
            }
        }
        return res;
    }       

    public boolean getSelectedTableType(int i) {
        return selectedTableTypes[i];
    }

    public void selectTableType(int i, boolean x) {
        selectedTableTypes[i] = x;
    }

    public void setColumnElements(SearchConditionElement[] columnElements) {
        this.columnElements = columnElements;
    }

    public void setTableElements(SearchConditionElement[] tableElements) {
        this.tableElements = tableElements;
    }

    public boolean showElementValueName(String typeValue) {
        for (int i = 0; i < TYPE_VALUES.length; i++) {
            if (TYPE_VALUES[i].equals(typeValue)) {
                return TYPE_NEED_VALUE1[i];
            }
        }
        throw new RuntimeException("Unknown type value: " + typeValue);
    }    

}
