package org.openl.studio.projects.service.tables.read;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import org.openl.rules.table.IOpenLTable;
import org.openl.studio.projects.model.tables.SimpleSpreadsheetView;
import org.openl.studio.projects.model.tables.SpreadsheetStepView;
import org.openl.studio.projects.service.tables.OpenLTableUtils;

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
        var metaInfoReader = tsn.getMetaInfoReader();
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
                    .value(getCellValue(tableBody.getCell(1, row), metaInfoReader));
            steps.add(stepBuilder.build());
        }
        builder.steps(steps);
    }
}
