package org.openl.excel.parser.dom;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openl.excel.parser.*;
import org.openl.rules.table.ICellComment;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.ui.ICellFont;
import org.openl.rules.table.ui.ICellStyle;
import org.openl.rules.table.xls.XlsCellComment;
import org.openl.rules.table.xls.XlsCellFont;
import org.openl.rules.table.xls.XlsCellStyle;
import org.openl.util.FileTool;
import org.openl.util.FileUtils;
import org.openl.util.NumberUtils;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DOMReader implements ExcelReader {
    private final Logger log = LoggerFactory.getLogger(DOMReader.class);

    private final String fileName;
    private File tempFile;
    private Workbook workbook;

    public DOMReader(String fileName) {
        this.fileName = fileName;
        ExcelUtils.configureZipBombDetection();
    }

    public DOMReader(InputStream is) {
        // Save to temp file because using an InputStream has a higher memory footprint than using a File. See POI javadocs.
        tempFile = FileTool.toTempFile(is, "stream.xls");
        this.fileName = tempFile.getAbsolutePath();
        ExcelUtils.configureZipBombDetection();
    }

    @Override
    public List<? extends SheetDescriptor> getSheets() throws ExcelParseException {
        try {
            initializeWorkbook();
            int numberOfSheets = workbook.getNumberOfSheets();
            List<DOMSheetDescriptor> sheets = new ArrayList<>(numberOfSheets);
            for (int i = 0; i < numberOfSheets; i++) {
                sheets.add(new DOMSheetDescriptor(workbook.getSheetName(i), i));
            }

            return sheets;
        } catch (IOException e) {
            throw new ExcelParseException(e);
        }
    }

    @Override
    public Object[][] getCells(SheetDescriptor sheet) throws ExcelParseException {
        DOMSheetDescriptor domSheet = (DOMSheetDescriptor) sheet;
        try {
            initializeWorkbook();
            Sheet sh = workbook.getSheet(sheet.getName());
            int firstRow = sh.getFirstRowNum();
            int lastRow = sh.getLastRowNum();

            // Find column dimensions
            int firstColumn = Integer.MAX_VALUE;
            int lastColumn = 0;
            for (int i = firstRow; i <= lastRow; i++) {
                Row row = sh.getRow(i);
                if (row == null) {
                    continue;
                }

                int firstCellNum = row.getFirstCellNum();
                if (firstCellNum >= 0 && firstCellNum < firstColumn) {
                    firstColumn = firstCellNum;
                }

                int lastCellNum = row.getLastCellNum() - 1;
                if (lastCellNum > lastColumn) {
                    lastColumn = lastCellNum;
                }
            }

            if (firstColumn == Integer.MAX_VALUE) {
                firstColumn = 0;
            }

            domSheet.setFirstRowNum(firstRow);
            domSheet.setFirstColNum(firstColumn);

            // Fill values
            int rows = lastRow - firstRow + 1;
            int cols = lastColumn - firstColumn + 1;
            log.debug("Array size: {}:{}", rows, cols);
            Object[][] cells = new Object[rows][cols];

            for (int i = firstRow; i <= lastRow; i++) {
                Row row = sh.getRow(i);
                if (row == null) {
                    continue;
                }
                int firstCellNum = row.getFirstCellNum();
                short lastCellNum = row.getLastCellNum();

                for (int j = firstCellNum; j < lastCellNum; j++) {
                    Cell cell = row.getCell(j);
                    Object value = extractCellValue(cell);
                    if (cell != null) {
                        short indention = cell.getCellStyle().getIndention();
                        if (indention > 0) {
                            value = new AlignedValue(value, indention);
                        }
                    }
                    cells[i - firstRow][j - firstColumn] = value;
                }
            }

            // Fill merged regions
            for (CellRangeAddress rangeAddress : sh.getMergedRegions()) {
                int firstMergeRow = rangeAddress.getFirstRow();
                int firstMergeCol = rangeAddress.getFirstColumn();
                int lastMergeRow = rangeAddress.getLastRow();
                int lastMergeCol = rangeAddress.getLastColumn();

                // Mark cells merged with Left. Don't include first column.
                for (int row = firstMergeRow; row <= lastMergeRow; row++) {
                    for (int col = firstMergeCol + 1; col <= lastMergeCol; col++) {
                        cells[row - firstRow][col - firstColumn] = MergedCell.MERGE_WITH_LEFT;
                    }
                }

                // Mark cells merged with Up. Only first column starting from second row.
                for (int row = firstMergeRow + 1; row <= lastMergeRow; row++) {
                    cells[row - firstRow][firstMergeCol - firstColumn] = MergedCell.MERGE_WITH_UP;
                }
            }

            return cells;
        } catch (IOException e) {
            throw new ExcelParseException(e);
        }
    }

    @Override
    public boolean isUse1904Windowing() throws ExcelParseException {
        try {
            initializeWorkbook();

            if (workbook instanceof XSSFWorkbook) {
                return ((XSSFWorkbook) workbook).isDate1904();
            } else if (workbook instanceof HSSFWorkbook) {
                return ((HSSFWorkbook) workbook).getInternalWorkbook().isUsing1904DateWindowing();
            }

            throw new UnsupportedOperationException("Unsupported workbook type");
        } catch (IOException e) {
            throw new ExcelParseException(e);
        }
    }

    @Override
    public TableStyles getTableStyles(final SheetDescriptor sheet, final IGridRegion tableRegion) {
        try {
            initializeWorkbook();

            // This is not optimal implementation. But because this class is used for comparing purposes only, it's ok.
            return new TableStyles() {
                @Override
                public IGridRegion getRegion() {
                    return tableRegion;
                }

                @Override
                public ICellStyle getStyle(int row, int column) {
                    return new XlsCellStyle(getCell(row, column).getCellStyle(), workbook);
                }

                @Override
                public ICellFont getFont(int row, int column) {
                    Font font = workbook.getFontAt(getCell(row, column).getCellStyle().getFontIndexAsInt());
                    return new XlsCellFont(font, workbook);
                }

                @Override
                public ICellComment getComment(int row, int column) {
                    return new XlsCellComment(getCell(row, column).getCellComment());
                }

                @SuppressWarnings("deprecation")
                @Override
                public String getFormula(int row, int column) {
                    Cell cell = getCell(row, column);
                    return cell.getCellType() == CellType.FORMULA ? cell.getCellFormula() : null;
                }

                private Cell getCell(int row, int column) {
                    return workbook.getSheet(sheet.getName()).getRow(row).getCell(column);
                }
            };
        } catch (IOException e) {
            throw new ExcelParseException(e);
        }
    }

    @Override
    public void close() {
        try {
            if (workbook != null) {
                workbook.close();
                workbook = null;
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }

        FileUtils.deleteQuietly(tempFile);
        tempFile = null;
    }

    private void initializeWorkbook() throws IOException {
        if (workbook == null) {
            // Open the file in read only mode
            workbook = WorkbookFactory.create(new File(fileName), null, true);
        }
    }

    // See OpenL Tablets implementation
    @SuppressWarnings("deprecation")
    private Object extractCellValue(Cell cell) {
        if (cell != null) {
            CellType type = cell.getCellType();
            if (type == CellType.FORMULA) {
                // Replace IGrid.CELL_TYPE_FORMULA with the type from the formula result
                type = cell.getCachedFormulaResultType();
            }
            // There IGrid.CELL_TYPE_FORMULA should never be at this step
            switch (type) {
                case BLANK:
                    return null;
                case BOOLEAN:
                    return cell.getBooleanCellValue();
                case NUMERIC:
                    if (DateUtil.isCellDateFormatted(cell)) {
                        return cell.getDateCellValue();
                    }
                    double value = cell.getNumericCellValue();
                    return NumberUtils.intOrDouble(value);
                case STRING:
                    return StringUtils.trimToNull(cell.getStringCellValue());
                default:
                    return "unknown type: " + cell.getCellType();
            }
        }
        return null;
    }

}
