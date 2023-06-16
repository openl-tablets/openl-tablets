package org.openl.rules.calc;

import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ILogicalTable;

/**
 * Extractor for values that are represented as column and row names in spreadsheet.
 *
 * @author DLiauchuk
 *
 */
public class CellsHeaderExtractor {
    private String[] rowNames;
    private String[] columnNames;

    /** table representing column section in the spreadsheet **/
    private final ILogicalTable columnNamesTable;

    /** table representing row section in the spreadsheet **/
    private final ILogicalTable rowNamesTable;

    public CellsHeaderExtractor(ILogicalTable columnNamesTable, ILogicalTable rowNamesTable) {
        this.columnNamesTable = columnNamesTable;
        this.rowNamesTable = rowNamesTable;
    }

    public ILogicalTable getColumnNamesTable() {
        return columnNamesTable;
    }

    public int getWidth() {
        return columnNamesTable == null ? 0 : columnNamesTable.getWidth();
    }

    public ILogicalTable getRowNamesTable() {
        return rowNamesTable;
    }

    public int getHeight() {
        return rowNamesTable == null ? 0 : rowNamesTable.getHeight();
    }

    public String[] getRowNames() {
        if (rowNames == null) {
            int height = getHeight();
            rowNames = new String[height];
            for (int row = 0; row < height; row++) {
                IGridTable nameCell = rowNamesTable.getRow(row).getColumn(0).getSource();
                rowNames[row] = nameCell.getCell(0, 0).getStringValue();
            }
        }
        return rowNames;
    }

    public String[] getColumnNames() {
        if (columnNames == null) {
            int width = getWidth();
            columnNames = new String[width];
            for (int col = 0; col < width; col++) {
                IGridTable nameCell = columnNamesTable.getColumn(col).getRow(0).getSource();
                columnNames[col] = nameCell.getCell(0, 0).getStringValue();
            }
        }
        return columnNames;
    }
}
