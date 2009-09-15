/**
 * Created May 9, 2007
 */
package org.openl.rules.search;

/**
 * @author snshor
 *
 */
public class ColumnGroupSelector extends ATableRowSelector implements ISearchConstants {

    class GroupResult {
        boolean groupRes = true;
        int currentIndex = 0;

        boolean execute(ISearchTableRow searchTableRow, ITableSearchInfo tableSearchInfo) {
            boolean res = true;
            while (currentIndex < tableElements.length) {
                int oldIndex = currentIndex;
                boolean x = executeGroup(searchTableRow, tableSearchInfo);
                res = tableElements[oldIndex].getGroupOperator().op(res, x);
            }

            return res;
        }

        boolean executeGroup(ISearchTableRow searchRatbleRow, ITableSearchInfo searchTableInfo) {
            boolean res = true;
            int elemLength = tableElements.length;
            for (int i = currentIndex; i < elemLength; ++i) {
                if (i > currentIndex && tableElements[i].getGroupOperator().isGroup()) {
                    currentIndex = i;
                    return res;
                }

                boolean x = selectors[i].isRowInTableSelected(searchRatbleRow, searchTableInfo);
                x = tableElements[i].isNotFlag() ? !x : x;
                res = i == currentIndex ? x : tableElements[i].getGroupOperator().op(res, x);

            }
            currentIndex = elemLength;
            return res;
        }
    }
    SearchConditionElement[] tableElements;

    ATableRowSelector[] selectors;

    /**
     * @param tableElements
     */
    public ColumnGroupSelector(SearchConditionElement[] tableElements) {
        this.tableElements = tableElements;
        init(tableElements);
    }

    void init(SearchConditionElement[] se) {
        selectors = new ATableRowSelector[se.length];
        for (int i = 0; i < se.length; i++) {
            ATableRowSelector tsnSel = makeTableRowSelector(se[i]);
            selectors[i] = tsnSel;
        }
    }

    /**
     * @param element
     * @return
     */
    private ATableRowSelector makeTableRowSelector(SearchConditionElement se) {
        if (COLUMN_PARAMETER.equals(se.getType())) {
            return new ColumnNameRowSelector(se);
        }
        if (COLUMN_TYPE.equals(se.getType())) {
            return new ColumnTypeRowSelector(se);
        }
        throw new RuntimeException("Unknown selector type: " + se.getType());
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.rules.search.ATableRowSelector#selectRowInTable(org.openl.rules.table.ITableRow,
     *      org.openl.rules.search.ITableSearchInfo)
     */
    @Override
    public boolean isRowInTableSelected(ISearchTableRow searchTableRow, ITableSearchInfo tableSearchInfo) {
        return new GroupResult().execute(searchTableRow, tableSearchInfo);
    }

}
