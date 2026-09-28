package org.openl.excel.grid;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

import org.openl.excel.parser.AlignedValue;
import org.openl.excel.parser.ExcelReaderFactory;
import org.openl.excel.parser.ExtendedValue;
import org.openl.excel.parser.MergedCell;
import org.openl.excel.parser.SheetDescriptor;
import org.openl.excel.parser.TableStyles;
import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.XlsWorkbookListener;
import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule;
import org.openl.rules.table.AGrid;
import org.openl.rules.table.GridRegion;
import org.openl.rules.table.ICell;
import org.openl.rules.table.ICellComment;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IWritableGrid;
import org.openl.rules.table.RegionsPool;
import org.openl.rules.table.ui.ICellFont;
import org.openl.rules.table.ui.ICellStyle;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.util.StringUtils;

@Slf4j
public class ParsedGrid extends AGrid {

    private final String workbookPath;
    @Getter(AccessLevel.PROTECTED)
    private final Object[][] cells;
    @Getter
    private final String uri;
    private final XlsSheetSourceCodeModule sheetSource;
    private final SheetDescriptor sheetDescriptor;
    @Getter(AccessLevel.PROTECTED)
    private final boolean use1904Windowing;
    private final List<IGridRegion> regions = new ArrayList<>();
    private final RegionsPool regionsPool = new RegionsPool();

    /**
     * The sheet taken up to be written, and by whom — read by every thread that reads a cell, so held as one
     * value that changes at once.
     */
    private final AtomicReference<Editing> editing = new AtomicReference<>();

    private IGridTable[] tables;
    private TableStyles currentTableStyles;

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
        var internalRow = row - getFirstRowNum();

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
    public boolean isEmpty(int col, int row) {
        var value = getCellValue(row, col);
        return value == null || value instanceof String s && StringUtils.isBlank(s);
    }

    @Override
    public IGridTable[] getTables() {
        tables = super.getTables();
        for (var t = 0; t < tables.length; t++) {
            tables[t] = new EditableGridTable(tables[t]);
        }
        return tables;
    }

    private void findRegions() {
        // This algorithm can be improved. Feel free to modify it if it becomes bottleneck.
        var startPoints = new LinkedHashSet<CellRowCol>();

        // Find top left points
        for (var i = 0; i < cells.length; i++) {
            var row = cells[i];
            for (var j = 0; j < row.length; j++) {
                var col = row[j];

                if (col instanceof MergedCell) {
                    var rowCol = findTopLeft(i, j);
                    startPoints.add(rowCol);
                }
            }
        }

        // Find bottom right points and create regions
        for (CellRowCol start : startPoints) {
            var end = findBottomRight(start.row, start.col);
            var region = new GridRegion(getFirstRowNum() + start.row,
                    getFirstColNum() + start.col,
                    getFirstRowNum() + end.row,
                    getFirstColNum() + end.col);
            regions.add(region);
            regionsPool.add(region);
        }
    }

    private CellRowCol findTopLeft(int internalRow, int internalCol) {
        while (cells[internalRow][internalCol] == MergedCell.MERGE_WITH_LEFT) {
            if (internalCol == 0) {
                break;
            }
            internalCol--;
        }
        while (cells[internalRow][internalCol] == MergedCell.MERGE_WITH_UP) {
            if (internalRow == 0) {
                break;
            }
            internalRow--;
        }
        return new CellRowCol(internalRow, internalCol);
    }

    private CellRowCol findBottomRight(int internalRow, int internalCol) {
        var endRow = internalRow;
        var endCol = internalCol;
        while (endRow < cells.length - 1 && cells[endRow + 1][endCol] == MergedCell.MERGE_WITH_UP) {
            endRow++;
        }
        while (endCol < cells[endRow].length - 1 && cells[endRow][endCol + 1] == MergedCell.MERGE_WITH_LEFT) {
            endCol++;
        }

        return new CellRowCol(endRow, endCol);
    }

    // Methods used in ParsedCell

    protected Object getCellValue(int row, int column) {
        var internalRow = row - getFirstRowNum();
        var internalCol = column - getFirstColNum();

        if (internalRow < 0 || internalCol < 0 || cells.length <= internalRow || cells[internalRow].length <= internalCol) {
            return null;
        }

        var value = cells[internalRow][internalCol];
        if (value instanceof MergedCell) {
            var topLeft = findTopLeft(internalRow, internalCol);
            value = cells[topLeft.row][topLeft.col];
        }
        if (value instanceof ExtendedValue extendedValue) {
            value = extendedValue.getValue();
        }

        return value;
    }

    protected ICellStyle getCellStyle(int row, int column) {
        var internalRow = row - getFirstRowNum();
        var internalCol = column - getFirstColNum();

        if (internalRow < 0 || internalCol < 0 || cells.length <= internalRow || cells[internalRow].length <= internalCol) {
            return null;
        }

        var value = cells[internalRow][internalCol];
        short indent = value instanceof AlignedValue av ? av.getIndent() : 0;
        return new IndentedStyle(indent, this, row, column);
    }

    protected TableStyles getTableStyles(int row, int column) {
        var internalRow = row - getFirstRowNum();
        var internalCol = column - getFirstColNum();

        if (internalRow >= 0 && internalCol >= 0 && cells.length > internalRow && cells[internalRow].length > internalCol) {
            var topLeft = findTopLeft(internalRow, internalCol);
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
            var region = table.getRegion();

            // Sometimes we need extra column and row to show the border of a table.
            // We need to know the styles of the cells lefter, above, righter and below the table.
            int left = region.getLeft() == 0 ? 0 : region.getLeft() - 1;
            int top = region.getTop() == 0 ? 0 : region.getTop() - 1;
            var extendedRegion = new GridRegion(top, left, region.getBottom() + 1, region.getRight() + 1);

            if (IGridRegion.Tool.contains(extendedRegion, column, row)) {
                try (var excelReader = ExcelReaderFactory.sequentialFactory().create(workbookPath)) {
                    styles = excelReader.getTableStyles(sheetDescriptor, extendedRegion);
                } catch (Exception e) {
                    // Fallback to empty style
                    log.error("Cannot read styles for sheet '{}'", sheetDescriptor.getName(), e);
                    styles = new EmptyTableStyles(extendedRegion);
                }

                break;
            }
        }
        return styles;
    }

    protected IGridRegion getRegion(int row, int col) {
        return regionsPool.getRegionContaining(col, row);
    }

    private int getFirstRowNum() {
        return sheetDescriptor.getFirstRowNum();
    }

    private int getFirstColNum() {
        return sheetDescriptor.getFirstColNum();
    }

    /**
     * Takes this sheet up to be written, and answers with the grid that writes into the workbook itself.
     *
     * <p>Whoever asks last is the one writing. Writers do not meet here: Studio runs them one at a time, and
     * asking again is how the next one takes over a sheet a failed write never said it had finished with.
     */
    protected IWritableGrid getWritableGrid() {
        var held = editing.get();
        var mine = Thread.currentThread().threadId();
        if (held != null && held.editor() == mine) {
            return held.grid();
        }
        var grid = held == null ? openForWriting() : held.grid();
        editing.set(new Editing(grid, mine));
        return grid;
    }

    private XlsSheetGridModel openForWriting() {
        sheetSource.getWorkbookSource().getWorkbookLoader().setCanUnload(false);
        var grid = new XlsSheetGridModel(sheetSource);
        // Prepare workbook for edit (load it to memory before editing starts)
        sheetSource.getSheet();
        return grid;
    }

    /**
     * The write has finished: the workbook may be unloaded again, and the sheet is read from what was parsed.
     *
     * <p>Let go of whoever was writing, not only of the caller: a save of the workbook says this of every sheet
     * in it, and a write left behind by a request that failed would otherwise keep the workbook in memory for
     * as long as the module lives.
     */
    protected void stopEditing() {
        if (editing.getAndSet(null) != null) {
            sheetSource.getWorkbookSource().getWorkbookLoader().setCanUnload(true);
        }
    }

    /**
     * The grid the thread asking is writing this sheet through, or {@code null} when it is not writing it.
     *
     * <p>Only the writer reads through the workbook, where its own unsaved cells are. Everybody else is answered
     * with nothing and reads the sheet as it was parsed: what is in the workbook is half of somebody's write,
     * and a POI workbook read while it is being written into corrupts the store both of them are on.
     */
    protected @Nullable IWritableGrid writableGridIfWriting() {
        var held = editing.get();
        return held != null && held.editor() == Thread.currentThread().threadId() ? held.grid() : null;
    }

    /**
     * A sheet taken up to be written, and the thread writing it.
     *
     * <p>The writer is named by its id rather than by the thread itself, so that a write which never said it had
     * finished keeps nothing alive.
     */
    private record Editing(XlsSheetGridModel grid, long editor) {
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static class CellRowCol {
        final int row;
        final int col;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            var that = (CellRowCol) o;
            return row == that.row && col == that.col;
        }

        @Override
        public int hashCode() {
            return Objects.hash(row, col);
        }
    }

    @RequiredArgsConstructor
    private static class EmptyTableStyles implements TableStyles {
        private final IGridRegion extendedRegion;

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
