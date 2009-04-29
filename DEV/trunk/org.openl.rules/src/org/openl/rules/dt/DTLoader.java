/*
 * Created on Oct 8, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.dt;

import java.util.ArrayList;
import java.util.List;

import org.openl.OpenL;
import org.openl.binding.IBindingContextDelegator;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.ILogicalTable;

/**
 * @author snshor
 *
 */
public class DTLoader implements IDecisionTableConstants, IXlsTableNames {

    RuleRow ruleRow;

    List<DTAction> actions = new ArrayList<DTAction>();

    int columns;

    List<DTCondition> conditions = new ArrayList<DTCondition>();

    void addAction(String name, int row, ILogicalTable table) {
        actions.add(new DTAction(name, row, table, false));

    }

    void addCondition(String name, int row, ILogicalTable table) {

        // ILogicalTable conditionRow = table.getLogicalRow(row);
        //
        // ILogicalTable codeTable = conditionRow.getLogicalColumn(CODE_COLUMN);
        // ILogicalTable paramTable =
        // conditionRow.getLogicalColumn(PARAM_COLUMN);
        //
        // ILogicalTable valuesTable = conditionRow.columns(DATA_COLUMN);

        conditions.add(new DTCondition(name, row, table));

    }

    void addReturnAction(String name, int row, ILogicalTable table) {
        actions.add(new DTAction(name, row, table, true));

    }

    void addRule(int row, ILogicalTable table) {
        if (ruleRow != null) {
            throw new RuntimeException("Only one rule row/column allowed");
        }
        ruleRow = new RuleRow(row, table);
    }

    int countConditionsAndActions(ILogicalTable t) {
        int w = t.getLogicalWidth();
        int cnt = 0;
        for (int i = 0; i < w; i++) {
            String s = t.getLogicalColumn(i).getGridTable().getCell(0, 0).getStringValue();

            if (s != null && s.length() >= 2) {
                cnt += (s.charAt(0) == 'C' || s.charAt(0) == 'A') && Character.isDigit(s.charAt(1)) ? 1 : 0;
            }
        }

        return cnt;

    }

    public DecisionTable load(TableSyntaxNode tsn, DecisionTable dt, OpenL openl, ModuleOpenClass module,
            IBindingContextDelegator cxtd) throws Exception {

        dt.setTableSyntaxNode(tsn);
        int startRow = tsn.getTableProperties() == null ? 1 : 2;
        ILogicalTable tableBody = tsn.getTable().rows(startRow);

        if (looksLikeTransposed(tableBody)) {
            tableBody = tableBody.transpose();
        }

        if (tableBody.getLogicalWidth() < DATA_COLUMN) {
            throw new Exception("Invalid structure of decision table");
        }

        columns = tableBody.getLogicalWidth() - DATA_COLUMN;

        ILogicalTable businessView = tableBody.columns(DATA_COLUMN - 1);
        tsn.getSubTables().put(VIEW_BUSINESS, businessView);

        for (int i = 0; i < tableBody.getLogicalHeight(); i++) {
            loadRow(i, tableBody);
        }

        // DecisionTableStructure dts = new DecisionTableStructure();
        // dts.setCondition(makeTemplates(conditions));
        // dts.setAction(makeTemplates(actions));

        dt.bindTable(conditions.toArray(new IDTCondition[0]), actions
                .toArray(new IDTAction[0]), ruleRow,
        // null,
                openl, module, cxtd, columns);

        return dt;
    }

    void loadRow(int row, ILogicalTable table) {

        String headerStr = table.getLogicalRow(row).getGridTable().getCell(INFO_COLUMN, 0).getStringValue();
        if (headerStr == null) {
            return;
        }

        String header = headerStr.toUpperCase();
        if (header.startsWith(CONDITION)) {
            addCondition(headerStr, row, table);
        } else if (header.startsWith(ACTION)) {
            addAction(headerStr, row, table);
        } else if (header.equals(RULE)) {
            addRule(row, table);
        } else if (header.startsWith(RETURN)) {
            addReturnAction(headerStr, row, table);
        }

        else {
            // throw new RuntimeException("Invalid type" + x[0]);
            // ignore for now
        }
        // return row + empty;

    }

    boolean looksLikeTransposed(ILogicalTable table) {
        if (table.getLogicalWidth() <= DATA_COLUMN) {
            return true;
        }

        if (table.getLogicalHeight() <= DATA_COLUMN) {
            return false;
        }

        int cnt1 = countConditionsAndActions(table);
        int cnt2 = countConditionsAndActions(table.transpose());

        if (cnt1 != cnt2) {
            return cnt1 > cnt2;
        }
        return table.getLogicalWidth() <= DATA_COLUMN;
    }

}