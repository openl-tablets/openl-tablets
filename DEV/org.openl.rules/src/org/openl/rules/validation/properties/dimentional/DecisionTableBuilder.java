package org.openl.rules.validation.properties.dimentional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.openl.rules.dt.DecisionTableColumnHeaders;
import org.openl.rules.dt.DecisionTableHelper;
import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.table.GridRegion;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.NullOpenClass;

/**
 * Creates the memory representation of DT table by POI.
 * 
 * @author DLiauchuk
 *
 */
public class DecisionTableBuilder {
    /** number 5 - is a number of first development rows in table. */
    static final int DECISION_TABLE_HEADER_ROWS_NUMBER = 5;
    static final int CONDITION_TITLE_ROW_INDEX = 4;
    static final int PARAMETER_DECLARATION_ROW_INDEX = 3;
    static final int CODE_EXPRESSION_ROW_INDEX = 2;
    /** condition name always is the next row after header row. */
    static final int COLUMN_TYPE_ROW_INDEX = 1;
    private static final String DISPATCHER_TABLES_SHEET_FORMAT = "$%sDispatcher Tables Sheet";
    private String methodName;
    private List<IDecisionTableColumn> conditions;
    private DispatcherTableReturnColumn returnColumn;

    private String tableName;
    private int rulesNumber;

    private static String buildMethodHeader(String tableName, DispatcherTableReturnColumn returnColumn) {

        final IMethodSignature originalSignature = returnColumn.getOriginalSignature();
        final StringBuilder builder = new StringBuilder(64);
        builder.append(IXlsTableNames.DECISION_TABLE2)
            .append(' ')
            .append(returnColumn.getReturnType().getDisplayName(0))
            .append(' ')
            .append(tableName)
            .append('(');

        boolean prependComma = false;
        // add original parameters of the method
        //
        for (int j = 0; j < originalSignature.getNumberOfParameters(); j++) {
            final IOpenClass parameterType = originalSignature.getParameterType(j);
            if (!(parameterType instanceof NullOpenClass) && parameterType.getInstanceClass() != null) {
                /*
                 * on compare in repository tutorial10, all original parameter types are instances of NullOpenClass. it
                 * causes NullPointerException. On compare we don`t need to build and execute validation tables at all
                 * during binding.
                 */
                if (prependComma) {
                    builder.append(',');
                }
                final String type = parameterType.getInstanceClass().getSimpleName();
                builder.append(type).append(" arg_").append(originalSignature.getParameterName(j));
                prependComma = true;
            }
        }

        // add new income parameters
        //
        for (Map.Entry<String, IOpenClass> param : TableSyntaxNodeDispatcherBuilder.INCOME_PARAMS.entrySet()) {
            if (prependComma) {
                builder.append(',');
            }
            final String type = param.getValue().getInstanceClass().getSimpleName();
            final String name = param.getKey();
            builder.append(type).append(' ').append(name);
            prependComma = true;
        }

        builder.append(')');
        return builder.toString();
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    void setConditions(List<IDecisionTableColumn> conditions) {
        this.conditions = conditions;
    }

    void setReturnColumn(DispatcherTableReturnColumn returnColumn) {
        this.returnColumn = returnColumn;
    }

    void setTableName(String tableName) {
        this.tableName = tableName;
    }

    void setRulesNumber(int rulesNumber) {
        this.rulesNumber = rulesNumber;
    }

    public XlsSheetGridModel build() {

        int numberOfAllLocalParameters = 0;
        for (IDecisionTableColumn condition : conditions) {
            if (condition.getNumberOfLocalParameters() > 0) {
                numberOfAllLocalParameters += condition.getNumberOfLocalParameters();
            }
        }
        int numberOfColumns = numberOfAllLocalParameters + 1; // + 1 for return column

        // TODO Excel has a maximum sheet name length limit. Find a solution for case when name is longer.
        String sheetName = String.format(DISPATCHER_TABLES_SHEET_FORMAT, methodName);
        XlsSheetGridModel sheetWithTable = DecisionTableHelper.createVirtualGrid(sheetName, numberOfColumns);

        // column index that is free for further writing
        int conditionsNumber = 0;
        List<IDecisionTableColumnBuilder> conditionBuilders = new ArrayList<>();
        for (IDecisionTableColumn condition : conditions) {
            if (condition.getNumberOfLocalParameters() > 0) {
                // process only conditions that have local parameters, other ones skip
                //
                conditionsNumber++;
                if (condition.getNumberOfLocalParameters() > 1) {
                    conditionBuilders.add(new ArrayConditionBuilder(condition, conditionsNumber));
                } else {
                    conditionBuilders.add(new SimpleConditionBuilder(condition, conditionsNumber));
                }
            }

        }

        int column = 0;
        for (IDecisionTableColumnBuilder conditionBuilder : conditionBuilders) {
            int columnUsedForCondition = conditionBuilder.build(sheetWithTable, rulesNumber, column, 0);
            column += columnUsedForCondition;
        }

        sheetWithTable.setCellValue(column, COLUMN_TYPE_ROW_INDEX, DecisionTableColumnHeaders.RETURN.getHeaderKey());
        sheetWithTable.setCellValue(column, CODE_EXPRESSION_ROW_INDEX, returnColumn.getCodeExpression());
        sheetWithTable.setCellValue(column, PARAMETER_DECLARATION_ROW_INDEX, returnColumn.getParameterDeclaration());
        sheetWithTable.setCellValue(column, CONDITION_TITLE_ROW_INDEX, returnColumn.getTitle());

        for (int i = 0; i < rulesNumber; i++) {
            sheetWithTable.setCellValue(column, i + DECISION_TABLE_HEADER_ROWS_NUMBER, returnColumn.getRuleValue(i));
        }

        String tableHeader = buildMethodHeader(tableName, returnColumn);
        sheetWithTable.setCellValue(0, 0, tableHeader);
        sheetWithTable.addMergedRegion(new GridRegion(0, 0, 0, column));

        return sheetWithTable;
    }

}
