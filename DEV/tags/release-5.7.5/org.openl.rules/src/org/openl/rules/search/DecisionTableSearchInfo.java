/**
 * Created Apr 29, 2007
 */
package org.openl.rules.search;

import java.util.ArrayList;

import org.openl.rules.dt.DecisionTable;
import org.openl.rules.dt.element.DecisionTableParameterInfo;
import org.openl.rules.dt.element.IAction;
import org.openl.rules.dt.element.ICondition;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.IGridTable;
import org.openl.types.IOpenClass;

/**
 * @author snshor
 *
 */
public class DecisionTableSearchInfo implements ITableSearchInfo {

    private TableSyntaxNode tsn;
    private DecisionTable dt;
    private DecisionTableParameterInfo[] params;

    public DecisionTableSearchInfo(TableSyntaxNode tsn) {
        this.tsn = tsn;
        dt = (DecisionTable) tsn.getMember();
    }

    public String getColumnDisplay(int col) {
        return getParams()[col].getPresentation();
    }

    public String getColumnName(int col) {
        return getParams()[col].getParameterDeclaration().getName();
    }

    public IOpenClass getColumnType(int col) {
        return getParams()[col].getParameterDeclaration().getType();
    }

    public DecisionTableParameterInfo[] getParams() {
        if (params == null) {
            ArrayList<DecisionTableParameterInfo> list = new ArrayList<DecisionTableParameterInfo>(20);
            for (int i = 0; i < dt.getConditionRows().length; i++) {
                ICondition c = dt.getConditionRows()[i];
                int n = c.numberOfParams();
                for (int j = 0; j < n; j++) {
                    list.add(c.getParameterInfo(j));
                }

            }

            for (int i = 0; i < dt.getActionRows().length; i++) {
                IAction a = dt.getActionRows()[i];
                int n = a.numberOfParams();
                for (int j = 0; j < n; j++) {
                    list.add(a.getParameterInfo(j));
                }
            }

            params = list.toArray(new DecisionTableParameterInfo[0]);

        }
        return params;
    }

    public TableSyntaxNode getTableSyntaxNode() {
        return tsn;
    }

    public IGridTable getHeaderDisplayTable() {
        return dt.getDisplayTable().getSource();
    }

    public int getNumberOfColumns() {
        return getParams().length;
    }

    public int getNumberOfRows() {
        return dt.getNumberOfRules();
    }

    public IGridTable getRowTable(int row) {
//        return dt.getRuleTable(row).getSourceTable();
        return dt.getRuleByIndex(row).getSource();
    }

    public Object getTableValue(int col, int row) {
        return getParams()[col].getValue(row);
    }

}
