package org.openl.rules.rest.service.tables.read;

import java.util.ArrayList;
import java.util.List;

import org.openl.rules.rest.model.tables.SimpleSpreadsheetView;
import org.openl.rules.rest.model.tables.SpreadsheetStepView;
import org.openl.rules.rest.service.tables.OpenLTableUtils;
import org.openl.rules.table.IOpenLTable;
import org.springframework.stereotype.Component;

/**
 * Reads simpple {@code Spreadsheet} table to {@link SimpleSpreadsheetView} model.
 *
 * @author Vladyslav Pikus
 */
@Component
public class SimpleSpreadsheetReader extends ExecutableTableReader<SimpleSpreadsheetView, SimpleSpreadsheetView.Builder> {

    public SimpleSpreadsheetReader() {
        super(SimpleSpreadsheetView::builder);
    }

    @Override
    public boolean supports(IOpenLTable table) {
        return OpenLTableUtils.isSimpleSpreadsheet(table);
    }

    @Override
    protected void initialize(SimpleSpreadsheetView.Builder builder, IOpenLTable openLTable) {
        super.initialize(builder, openLTable);

        var tsn = openLTable.getSyntaxNode();
        var tableBody = tsn.getTableBody();
        int height = OpenLTableUtils.getHeightWithoutEmptyRows(tableBody);
        int width = OpenLTableUtils.getWidthWithoutEmptyColumns(tableBody);
        if (height == 2 && width > 2) {
            tableBody = tableBody.transpose();
            height = OpenLTableUtils.getHeightWithoutEmptyRows(tableBody);
        }

        List<SpreadsheetStepView> steps = new ArrayList<>();
        for (int row = 1; row < height; row++) {
            var stepParts = SpreadsheetTableReader.splitStepDeclaration(tableBody.getCell(0, row).getStringValue());
            var stepBuilder = SpreadsheetStepView.builder()
                .name(stepParts.getLeft())
                .type(stepParts.getRight())
                .value(tableBody.getCell(1, row).getObjectValue());
            steps.add(stepBuilder.build());
        }
        builder.steps(steps);
    }
}
