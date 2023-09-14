package org.openl.rules.rest.service.tables.write;

import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.rest.model.tables.SimpleSpreadsheetView;
import org.openl.rules.table.IOpenLTable;
import org.openl.util.StringUtils;

/**
 * TODO description
 *
 * @author Vladyslav Pikus
 */
public class SimpleSpreadsheetTableWriter extends ExecutableTableWriter<SimpleSpreadsheetView> {

    private static final char TYPE_DELIM = ':';

    private static final int STEP_NAME_COLUMN = 0;
    private static final int STEP_VALUE_COLUMN = 1;

    public SimpleSpreadsheetTableWriter(IOpenLTable table) {
        super(table);
    }

    @Override
    protected void updateBusinessBody(SimpleSpreadsheetView tableView) {
        var tableBody = table.getGridTable(IXlsTableNames.VIEW_BUSINESS);

        int row = 0;
        createOrUpdateCell(tableBody, buildCellKey(STEP_NAME_COLUMN, row), "Steps");
        createOrUpdateCell(tableBody, buildCellKey(STEP_VALUE_COLUMN, row), "Value");
        row++;
        for (var step : tableView.steps) {
            createOrUpdateCell(tableBody, buildCellKey(STEP_NAME_COLUMN, row), createStep(step.name, step.type));
            createOrUpdateCell(tableBody, buildCellKey(STEP_VALUE_COLUMN, row), step.value);
            row++;
        }
    }

    public static String createStep(String name, String type) {
        var stepDeclaration = name;
        if (StringUtils.isNotBlank(type)) {
            stepDeclaration += TYPE_DELIM + type;
        }
        return stepDeclaration;
    }
}
