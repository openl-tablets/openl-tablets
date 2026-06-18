package org.openl.studio.projects.model.tables;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * Request model for appending rows to {@code Spreadsheet} table
 *
 * @author Vladyslav Pikus
 */
@Getter
@Setter
public class SpreadsheetAppend implements AppendTableView {

    @Schema(description = "Collection of spreadsheet rows to append")
    private List<SpreadsheetRowView> rows;

    @Schema(description = "2D array of spreadsheet cells to append; one row of cells per appended row")
    private SpreadsheetCellView[][] cells;

    @Override
    public String getTableType() {
        return SpreadsheetView.TABLE_TYPE;
    }

    public void setTableType(String tableType) {
        // no op
    }
}
