/**
 * Created May 9, 2007
 */
package org.openl.rules.search;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;

/**
 * Adds the possibility to combine table selectors to groups.
 * @author snshor
 *
 */
public class TableGroupSelector extends ATableSyntaxNodeSelector implements ISearchConstants {
    
    protected SearchConditionElement[] tableElements;

    protected ATableSyntaxNodeSelector[] selectors;
    
    class GroupResult {
        boolean groupRes = true;
        int idx = 0;

        boolean execute(Object target) {
            boolean res = true;
            while (idx < tableElements.length) {
                int oldIdx = idx;
                boolean x = executeGroup(target);
                res = tableElements[oldIdx].getGroupOperator().op(res, x);
            }

            return res;
        }

        boolean executeGroup(Object target) {
            boolean res = true;
            int N = tableElements.length;
            for (int i = idx; i < N; ++i) {
                if (i > idx && tableElements[i].getGroupOperator().isGroup()) {
                    idx = i;
                    return res;
                }

                boolean x = selectors[i].select((TableSyntaxNode)target);
                x = tableElements[i].isNotFlag() ? !x : x;
                res = i == idx ? x : tableElements[i].getGroupOperator().op(res, x);

            }
            idx = N;
            return res;

        }
    }

    /**
     * @param tableElements
     */
    public TableGroupSelector(SearchConditionElement[] tableElements) {
        this.tableElements = tableElements;
        init(tableElements);
    }

    void init(SearchConditionElement[] se) {
        selectors = new ATableSyntaxNodeSelector[se.length];
        for (int i = 0; i < se.length; i++) {
            ATableSyntaxNodeSelector tsnSel = makeTableSyntaxNodeSelector(se[i]);
            selectors[i] = tsnSel;
        }
    }

    /**
     * @param element
     * @return
     */
    private ATableSyntaxNodeSelector makeTableSyntaxNodeSelector(SearchConditionElement se) {
        if (HEADER.equals(se.getType())) {
            return new TableHeaderSelector(se);
        }
        if (PROPERTY.equals(se.getType())) {
            return new PropertySelector(se);
        }
        throw new RuntimeException("Unknown selector type: " + se.getType());
    }

    @Override
    public boolean doesTableMatch(TableSyntaxNode node) {
        return new GroupResult().execute(node);

    }

}
