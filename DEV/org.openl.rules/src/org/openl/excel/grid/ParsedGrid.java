package org.openl.excel.grid;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

import org.openl.excel.parser.*;
import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.XlsWorkbookListener;
import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule;
import org.openl.rules.table.*;
import org.openl.rules.table.ui.ICellFont;
import org.openl.rules.table.ui.ICellStyle;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParsedGrid extends AGrid {
    private final Logger log = LoggerFactory.getLogger(ParsedGrid.class);

    private final String workbookPath;
    private final Object[][] cells;
    private final String uri;
    private final XlsSheetSourceCodeModule sheetSource;
    private final SheetDescriptor sheetDescriptor;
    private final boolean use1904Windowing;
    private final List<IGridRegion> regions = new ArrayList<>();

    private XlsSheetGridModel writableGrid;

    private transient IGridTable[] tables;
    private transient TableStyles currentTableStyles;

    ParsedGrid(String workbookPath,
            XlsSheetSourceCodeModule sheetSource,
            SheetDescriptor sheet,
            Object[][] cells,
            boolean use1904Windowing) {
        this.workbookPath = workbookPath;
        this.cells = cells;
        this.sheetSource = sheetSource;
        this.uri = sheetSource.getUri();
        this.sheetDescriptor = sheet;
        this.use1904Windowing = use1904Windowing;

        findRegions();

        sheetSource.getWorkbookSource().addListener(new WorkbookSaveListener());
    }

    @Override
    public ICell getCell(int column, int row) {
        return new ParsedCell(row, column, this);
    }

    @Override
    public int getColumnWidth(int i) {
        return 0;
    }

    @Override
    public int getMaxColumnIndex(int row) {
        int internalRow = row - getFirstRowNum();

        if (cells.length <= internalRow) {
            return 0;
        }
        return getFirstColNum() + cells[internalRow].length - 1;
    }

    @Override
    public int getMaxRowIndex() {
        return getFirstRowNum() + cells.length - 1;
    }

    @Override
    public IGridRegion getMergedRegion(int i) {
        return regions.get(i);
    }

    @Override
    public int getMinColumnIndex(int row) {
        return getFirstColNum();
    }

    @Override
    public int getMinRowIndex() {
        return getFirstRowNum();
    }

    @Override
    public int getNumberOfMergedRegions() {
        return regions.size();
    }

    @Override
    public String getUri() {
        return uri;
    }

    @Override
    public boolean isEmpty(int col, int row) {
        Object value = getCellValue(row, col);
        return value == null || value instanceof String && StringUtils.isBlank((String) value);
    }

    @Override
    public IGridTable[] getTables() {
        tables = super.getTables();
        for (int t = 0; t < tables.length; t++) {
            tables[t] = new EditableGridTable(tables[t]);
        }
        return tables;
    }

    private void findRegions() {
        // This algorithm can be improved. Feel free to modify it if it becomes bottleneck.
        LinkedHashSet<CellRowCol> startPoints = new LinkedHashSet<>();

        // Find top left points
        for (int i = 0; i < cells.length; i++) {
            Object[] row = cells[i];
            for (int j = 0; j < row.length; j++) {
                Object col = row[j];

                if (col instanceof MergedCell) {
                    CellRowCol rowCol = findTopLeft(i, j);
                    startPoints.add(rowCol);
                }
            }
        }

        // Find bottom right points and create regions
        for (CellRowCol start : startPoints) {
            CellRowCol end = findBottomRight(start.row, start.col);
            regions.add(new GridRegion(getFirstRowNum() + start.row,
                    getFirstColNum() + start.col,
                    getFirstRowNum() + end.row,
                    getFirstColNum() + end.col));
        }
    }

    private CellRowCol findTopLeft(int internalRow, int internalCol) {
        while (cells[internalRow][internalCol] == MergedCell.MERGE_WITH_LEFT) {
            internalCol--;
        }
        while (cells[internalRow][internalCol] == MergedCell.MERGE_WITH_UP) {
            internalRow--;
        }
        return new CellRowCol(internalRow, internalCol);
    }

    private CellRowCol findBottomRight(int internalRow, int internalCol) {
        int endRow = internalRow;
        int endCol = internalCol;
        while (endRow < cells.length - 1 && cells[endRow + 1][endCol] == MergedCell.MERGE_WITH_UP) {
            endRow++;
        }
        while (endCol < cells[endRow].length - 1 && cells[endRow][endCol + 1] == MergedCell.MERGE_WITH_LEFT) {
            endCol++;
        }

        return new CellRowCol(endRow, endCol);
    }

    /////////////////////////// Methods used in ParsedCell ///////////////////////////////////

    protected Object getCellValue(int row, int column) {
        int internalRow = row - getFirstRowNum();
        int internalCol = column - getFirstColNum();

        if (internalRow < 0 || internalCol < 0 || cells.length <= internalRow || cells[internalRow].length <= internalCol) {
            return null;
        }

        Object value = cells[internalRow][internalCol];
        if (value instanceof MergedCell) {
            CellRowCol topLeft = findTopLeft(internalRow, internalCol);
            value = cells[topLeft.row][topLeft.col];
        }
        if (value instanceof ExtendedValue) {
            value = ((ExtendedValue) value).getValue();
        }

        return value;
    }

    protected ICellStyle getCellStyle(int row, int column) {
        int internalRow = row - getFirstRowNum();
        int internalCol = column - getFirstColNum();

        if (internalRow < 0 || internalCol < 0 || cells.length <= internalRow || cells[internalRow].length <= internalCol) {
            return null;
        }

        Object value = cells[internalRow][internalCol];
        short indent = value instanceof AlignedValue ? ((AlignedValue) value).getIndent() : 0;
        return new IndentedStyle(indent, this, row, column);
    }

    protected TableStyles getTableStyles(int row, int column) {
        int internalRow = row - getFirstRowNum();
        int internalCol = column - getFirstColNum();

        if (internalRow >= 0 && internalCol >= 0 && cells.length > internalRow && cells[internalRow].length > internalCol) {
            CellRowCol topLeft = findTopLeft(internalRow, internalCol);
            row -= internalRow - topLeft.row;
            column -= internalCol - topLeft.col;
        }

        if (currentTableStyles == null || !IGridRegion.Tool.contains(currentTableStyles.getRegion(), column, row)) {
            currentTableStyles = readTableStyles(row, column);
        }

        return currentTableStyles;
    }

    private TableStyles readTableStyles(int row, int column) {
        if (workbookPath == null) {
            // No need to show styles in read only mode (when access workbook through stream)
            return null;
        }

        TableStyles styles = null;
        for (IGridTable table : tables) {
            IGridRegion region = table.getRegion();

            // Sometimes we need extra column and row to show the border of a table.
            // We need to know the styles of the cells lefter, above, righter and below the table.
            int left = region.getLeft() == 0 ? 0 : region.getLeft() - 1;
            int top = region.getTop() == 0 ? 0 : region.getTop() - 1;
            IGridRegion extendedRegion = new GridRegion(top, left, region.getBottom() + 1, region.getRight() + 1);

            if (IGridRegion.Tool.contains(extendedRegion, column, row)) {
                try (ExcelReader excelReader = ExcelReaderFactory.sequentialFactory().create(workbookPath)) {
                    styles = excelReader.getTableStyles(sheetDescriptor, extendedRegion);
                } catch (Exception e) {
                    // Fallback to empty style
                    log.error("Can't read styles for sheet '{}'", sheetDescriptor.getName(), e);
                    styles = new EmptyTableStyles(extendedRegion);
                }

                break;
            }
        }
        return styles;
    }

    protected IGridRegion getRegion(int row, int col) {
        for (IGridRegion region : regions) {
            if (region.getTop() <= row && row <= region.getBottom() &&
                    region.getLeft() <= col && col <= region.getRight()) {
                return region;
            }
        }

        return null;
    }

    protected Object[][] getCells() {
        return cells;
    }

    private int getFirstRowNum() {
        return sheetDescriptor.getFirstRowNum();
    }

    private int getFirstColNum() {
        return sheetDescriptor.getFirstColNum();
    }

    protected boolean isUse1904Windowing() {
        return use1904Windowing;
    }

    protected IWritableGrid getWritableGrid() {
        if (writableGrid == null) {
            sheetSource.getWorkbookSource().getWorkbookLoader().setCanUnload(false);
            writableGrid = new XlsSheetGridModel(sheetSource);
            // Prepare workbook for edit (load it to memory before editing starts)
            sheetSource.getSheet();
        }
        return writableGrid;
    }

    protected void stopEditing() {
        if (isEditing()) {
            sheetSource.getWorkbookSource().getWorkbookLoader().setCanUnload(true);
            writableGrid = null;
        }
    }

    protected boolean isEditing() {
        return writableGrid != null;
    }

    //////////////////////////////////////////////////////////////////////

    private static class CellRowCol {
        final int row;
        final int col;

        private CellRowCol(int row, int col) {
            this.row = row;
            this.col = col;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            CellRowCol that = (CellRowCol) o;
            return row == that.row &&
                    col == that.col;
        }

        @Override
        public int hashCode() {
            return Objects.hash(row, col);
        }
    }

    private static class EmptyTableStyles implements TableStyles {
        private final IGridRegion extendedRegion;

        public EmptyTableStyles(IGridRegion extendedRegion) {
            this.extendedRegion = extendedRegion;
        }

        @Override
        public IGridRegion getRegion() {
            return extendedRegion;
        }

        @Override
        public ICellStyle getStyle(int row, int column) {
            return null;
        }

        @Override
        public ICellFont getFont(int row, int column) {
            return null;
        }

        @Override
        public ICellComment getComment(int row, int column) {
            return null;
        }

        @Override
        public String getFormula(int row, int column) {
            return null;
        }
    }

    private class WorkbookSaveListener implements XlsWorkbookListener {
        @Override
        public void beforeSave(XlsWorkbookSourceCodeModule workbookSourceCodeModule) {
            // Do nothing
        }

        @Override
        public void afterSave(XlsWorkbookSourceCodeModule workbookSourceCodeModule) {
            stopEditing();
        }
    }
}
