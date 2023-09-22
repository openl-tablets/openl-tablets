package org.openl.rules.rest.service.tables.read;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.tuple.Pair;
import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.rest.model.tables.SpreadsheetCellView;
import org.openl.rules.rest.model.tables.SpreadsheetColumnView;
import org.openl.rules.rest.model.tables.SpreadsheetRowView;
import org.openl.rules.rest.model.tables.SpreadsheetView;
import org.openl.rules.rest.service.tables.OpenLTableUtils;
import org.openl.rules.table.IOpenLTable;
import org.openl.util.StringUtils;
import org.springframework.stereotype.Component;

/**
 * Reads legacy {@code Spreadsheet} table to {@link SpreadsheetView} model.
 *
 * @author Vladyslav Pikus
 */
@Component
public class SpreadsheetTableReader extends ExecutableTableReader<SpreadsheetView, SpreadsheetView.Builder> {

    public SpreadsheetTableReader() {
        super(SpreadsheetView::builder);
    }

    @Override
    public boolean supports(IOpenLTable table) {
        return OpenLTableUtils.isSpreadsheetTable(table) && !OpenLTableUtils.isSimpleSpreadsheet(table);
    }

    @Override
    protected void initialize(SpreadsheetView.Builder builder, IOpenLTable openLTable) {
        super.initialize(builder, openLTable);

        var tsn = openLTable.getSyntaxNode();
        var tableBody = tsn.getTableBody();
        int height = OpenLTableUtils.getHeightWithoutEmptyRows(tableBody);
        int width = OpenLTableUtils.getWidthWithoutEmptyColumns(tableBody);

        List<SpreadsheetRowView> rows = new ArrayList<>(height - 1);
        for (int row = 1; row < height; row++) {
            var rowParts = splitStepDeclaration(tableBody.getCell(0, row).getStringValue());
            var rowBuilder = SpreadsheetRowView.builder().name(rowParts.getLeft()).type(rowParts.getRight());
            rows.add(rowBuilder.build());
        }
        builder.rows(rows);
        List<SpreadsheetColumnView> columns = new ArrayList<>(width - 1);
        for (int col = 1; col < width; col++) {
            var colParts = splitStepDeclaration(tableBody.getCell(col, 0).getStringValue());
            var colBuilder = SpreadsheetColumnView.builder().name(colParts.getLeft()).type(colParts.getRight());
            columns.add(colBuilder.build());
        }
        builder.columns(columns);
        SpreadsheetCellView[][] cells = new SpreadsheetCellView[height - 1][width - 1];
        for (int row = 1; row < height; row++) {
            for (int col = 1; col < width; col++) {
                var cellBuilder = SpreadsheetCellView.builder().value(tableBody.getCell(col, row).getObjectValue());
                cells[row - 1][col - 1] = cellBuilder.build();
            }
        }
        builder.cells(cells);
    }

    public static Pair<String, String> splitStepDeclaration(String stepName) {
        if (stepName == null) {
            return Pair.of(null, null);
        }
        var typeDelimPos = stepName.indexOf(':');
        if (typeDelimPos > -1) {
            return Pair.of(StringUtils.trimToNull(stepName.substring(0, typeDelimPos)),
                    StringUtils.trimToNull(stepName.substring(typeDelimPos + 1)));
        } else {
            return Pair.of(StringUtils.trimToNull(stepName), null);
        }
    }
}
