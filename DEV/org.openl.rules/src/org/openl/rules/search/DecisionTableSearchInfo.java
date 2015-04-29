/**
 * Created Apr 29, 2007
 */
package org.openl.rules.search;

import java.util.ArrayList;

import org.openl.rules.dtx.IBaseAction;
import org.openl.rules.dtx.IBaseCondition;
import org.openl.rules.dtx.IDecisionTable;
import org.openl.rules.dtx.IDecisionTableParameterInfo;
import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.IGridTable;
import org.openl.types.IOpenClass;

/**
 * @author snshor
 *
 */
public class DecisionTableSearchInfo implements ITableSearchInfo {

    private TableSyntaxNode tsn;
    private IDecisionTable dt;
    private IDecisionTableParameterInfo[] params;

    public DecisionTableSearchInfo(TableSyntaxNode tsn) {
        this.tsn = tsn;
        dt = (IDecisionTable) tsn.getMember();
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

    public IDecisionTableParameterInfo[] getParams() {
        if (params == null) {
            ArrayList<IDecisionTableParameterInfo> list = new ArrayList<IDecisionTableParameterInfo>(20);
            for (int i = 0; i < dt.getConditionRows().length; i++) {
                IBaseCondition c = dt.getConditionRows()[i];
                int n = c.getNumberOfParams();
                for (int j = 0; j < n; j++) {
                    list.add(c.getParameterInfo(j));
                }

            }

            for (int i = 0; i < dt.getActionRows().length; i++) {
                IBaseAction a = dt.getActionRows()[i];
                int n = a.getNumberOfParams();
                for (int j = 0; j < n; j++) {
                    list.add(a.getParameterInfo(j));
                }
            }

            params = list.toArray(new IDecisionTableParameterInfo[0]);

        }
        return params;
    }

    public TableSyntaxNode getTableSyntaxNode() {
        return tsn;
    }

    public IGridTable getHeader() {
        return dt.getSyntaxNode().getTable(IXlsTableNames.VIEW_BUSINESS).getRow(0).getSource();
    }

    public int getNumberOfColumns() {
        return getParams().length;
//    	return dt.getTotalNumberOfParams();
    	
    }

    public int getNumberOfRows() {
        return dt.getNumberOfRules();
    }

    public IGridTable getRowTable(int row) {
        return dt.getRuleTable(row).getSource();
    }

    public Object getTableValue(int col, int row) {
        return getParams()[col].getValue(row);
//        return  dt.getParamValue(col, row);
    }

}
