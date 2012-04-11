/**
 * Created Apr 29, 2007
 */
package org.openl.rules.search;

import java.util.ArrayList;

import org.openl.rules.dt.DTParameterInfo;
import org.openl.rules.dt.DecisionTable;
import org.openl.rules.dt.IDTAction;
import org.openl.rules.dt.IDTCondition;
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
    private DTParameterInfo[] params;

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

    public DTParameterInfo[] getParams() {
        if (params == null) {
            ArrayList<DTParameterInfo> list = new ArrayList<DTParameterInfo>(20);
            for (int i = 0; i < dt.getConditionRows().length; i++) {
                IDTCondition c = dt.getConditionRows()[i];
                int n = c.numberOfParams();
                for (int j = 0; j < n; j++) {
                    list.add(c.getParameterInfo(j));
                }

            }

            for (int i = 0; i < dt.getActionRows().length; i++) {
                IDTAction a = dt.getActionRows()[i];
                int n = a.numberOfParams();
                for (int j = 0; j < n; j++) {
                    list.add(a.getParameterInfo(j));
                }
            }

            params = list.toArray(new DTParameterInfo[0]);

        }
        return params;
    }

    public TableSyntaxNode getTableSyntaxNode() {
        return tsn;
    }

    public IGridTable getHeaderDisplayTable() {
        return dt.getDisplayTable().getGridTable();
    }

    public int getNumberOfColumns() {
        return getParams().length;
    }

    public int getNumberOfRows() {
        return dt.getNumberOfRules();
    }

    public IGridTable getRowTable(int row) {
        return dt.getRuleTable(row).getGridTable();
    }

    public Object getTableValue(int col, int row) {
        return getParams()[col].getValue(row);
    }

}
