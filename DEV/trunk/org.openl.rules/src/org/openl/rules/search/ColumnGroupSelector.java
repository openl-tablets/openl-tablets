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
        int idx = 0;

        boolean execute(ISearchTableRow row, ITableSearchInfo tsi) {
            boolean res = true;
            while (idx < tableElements.length) {
                int oldIdx = idx;
                boolean x = executeGroup(row, tsi);
                res = tableElements[oldIdx].getOperator().op(res, x);
            }

            return res;
        }

        boolean executeGroup(ISearchTableRow row, ITableSearchInfo tsi) {
            boolean res = true;
            int N = tableElements.length;
            for (int i = idx; i < N; ++i) {
                if (i > idx && tableElements[i].getOperator().isGroup()) {
                    idx = i;
                    return res;
                }

                boolean x = selectors[i].selectRowInTable(row, tsi);
                x = tableElements[i].isNotFlag() ? !x : x;
                res = i == idx ? x : tableElements[i].getOperator().op(res, x);

            }
            idx = N;
            return res;
        }
    }
    SearchElement[] tableElements;

    ATableRowSelector[] selectors;

    /**
     * @param tableElements
     */
    public ColumnGroupSelector(SearchElement[] tableElements) {
        this.tableElements = tableElements;
        init(tableElements);
    }

    void init(SearchElement[] se) {
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
    private ATableRowSelector makeTableRowSelector(SearchElement se) {
        if (COLUMN_NAME.equals(se.getType())) {
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
    public boolean selectRowInTable(ISearchTableRow row, ITableSearchInfo tsi) {
        return new GroupResult().execute(row, tsi);
    }

}
