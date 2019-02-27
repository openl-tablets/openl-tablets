package org.openl.rules.dt.builder;

import org.openl.rules.table.GridRegion;
import org.openl.rules.table.IWritableGrid;

/**
 * Builds the header of the table in the given sheet. Merges the appropriate region.
 * 
 * @author DLiauchuk
 *
 */
public class TableHeaderBuilder {

    private String tableHeader;

    public TableHeaderBuilder(String tableHeader) {
        this.tableHeader = tableHeader;
    }

    /**
     * Builds the header of the table in the given sheet. Merges the appropriate region.
     * 
     * @param sheet sheet for header writing
     * @param lastTableColumnNumber the number of the last column of the table. Is needed to merge the header region.
     * @param columnStartIndex the start index of the column on the sheet where header is starting
     * @param rowStartIndex the start index of the row on the sheet where header is starting
     * 
     */
    public void build(IWritableGrid sheet, int lastTableColumnNumber, int columnStartIndex, int rowStartIndex) {
        sheet.setCellValue(columnStartIndex, rowStartIndex, tableHeader);

        sheet.addMergedRegion(new GridRegion(rowStartIndex, columnStartIndex, rowStartIndex, lastTableColumnNumber));
    }
}
