/*
 * Created on Sep 19, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.table.xls;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.openl.domain.EnumDomain;
import org.openl.domain.IDomain;
import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule;
import org.openl.rules.lang.xls.types.CellMetaInfo;
import org.openl.rules.table.AGridModel;
import org.openl.rules.table.CellKey;
import org.openl.rules.table.GridRegion;
import org.openl.rules.table.ICell;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IWritableGrid;
import org.openl.rules.table.RegionsPool;
import org.openl.rules.table.ui.ICellStyle;
import org.openl.rules.table.xls.writers.AXlsCellWriter;
import org.openl.rules.table.xls.writers.XlsCellArrayWriter;
import org.openl.rules.table.xls.writers.XlsCellBooleanWriter;
import org.openl.rules.table.xls.writers.XlsCellDateWriter;
import org.openl.rules.table.xls.writers.XlsCellEnumArrayWriter;
import org.openl.rules.table.xls.writers.XlsCellEnumWriter;
import org.openl.rules.table.xls.writers.XlsCellFormulaWriter;
import org.openl.rules.table.xls.writers.XlsCellNumberWriter;
import org.openl.rules.table.xls.writers.XlsCellStringWriter;
import org.openl.types.IOpenClass;
import org.openl.util.EnumUtils;
import org.openl.util.StringTool;

/**
 * @author snshor
 * 
 */
public class XlsSheetGridModel extends AGridModel implements IWritableGrid, XlsWorkbookSourceCodeModule.WorkbookListener {

    private XlsSheetSourceCodeModule sheetSource;

    private Sheet sheet;
    private RegionsPool mergedRegionsPool;

    private Map<CellKey, CellMetaInfo> metaInfoMap = new HashMap<CellKey, CellMetaInfo>();

    private Map<String, AXlsCellWriter> cellWriters = new HashMap<String, AXlsCellWriter>();    

    // private final static Log LOG =
    // LogFactory.getLog(XlsSheetGridModel.class);
    
    // TODO: move to some helper class.
    public static int getColumn(String cell) {
        int col = 0;
        int mul = 'Z' - 'A' + 1;
        for (int i = 0; i < cell.length(); i++) {
            char ch = cell.charAt(i);
            if (!Character.isLetter(ch)) {
                return col - 1;
            }
            col = col * mul + ch - 'A' + 1;
        }
        throw new RuntimeException("Invalid cell: " + cell);
    }
    
    // TODO: move to some helper class.
    public static int getRow(String cell) {
        for (int i = 0; i < cell.length(); i++) {
            char ch = cell.charAt(i);
            if (Character.isDigit(ch)) {
                return Integer.parseInt(cell.substring(i)) - 1;
            }
        }
        throw new RuntimeException("Invalid cell: " + cell);
    }
    
    // TODO: move to some helper class.
    public static IGridRegion makeRegion(String range) {

        int idx = range.indexOf(RANGE_SEPARATOR);
        if (idx < 0) {
            int col1 = getColumn(range);
            int row1 = getRow(range);
            return new GridRegion(row1, col1, row1, col1);
        }
        String[] rr = StringTool.tokenize(range, RANGE_SEPARATOR);

        int col1 = getColumn(rr[0]);
        int row1 = getRow(rr[0]);
        int col2 = getColumn(rr[1]);
        int row2 = getRow(rr[1]);

        return new GridRegion(row1, col1, row2, col2);
    }

    public XlsSheetGridModel(Sheet sheet) {
        this.sheet = sheet;
        extractMergedRegions();
        initCellWriters();
    }

    public XlsSheetGridModel(XlsSheetSourceCodeModule sheetSource) {
        this.sheetSource = sheetSource;
        sheet = sheetSource.getSheet();
        extractMergedRegions();

        sheetSource.getWorkbookSource().addListener(this);
        initCellWriters();
    }

    private void extractMergedRegions() {
        mergedRegionsPool = new RegionsPool(null);
        int nregions = getNumberOfMergedRegions();
        for (int i = 0; i < nregions; i++) {
            CellRangeAddress reg = getMergedRegionAt(i);
            mergedRegionsPool.add(new XlsGridRegion(reg));
        }
    }
    
    // TODO: move to factory class.
    private void initCellWriters() {
        cellWriters.put(AXlsCellWriter.ARRAY_WRITER, new XlsCellArrayWriter(this));
        cellWriters.put(AXlsCellWriter.BOOLEAN_WRITER, new XlsCellBooleanWriter(this));
        cellWriters.put(AXlsCellWriter.DATE_WRITER, new XlsCellDateWriter(this));
        cellWriters.put(AXlsCellWriter.ENUM_ARRAY_WRITER, new XlsCellEnumArrayWriter(this));
        cellWriters.put(AXlsCellWriter.ENUM_WRITER, new XlsCellEnumWriter(this));
        cellWriters.put(AXlsCellWriter.FORMULA_WRITER, new XlsCellFormulaWriter(this));
        cellWriters.put(AXlsCellWriter.NUMBER_WRITER, new XlsCellNumberWriter(this));
        cellWriters.put(AXlsCellWriter.STRING_WRITER, new XlsCellStringWriter(this));
    }

    public int addMergedRegion(IGridRegion reg) {
        Object topLeftCellValue = findFirstValueInRegion(reg);
        for (int row = reg.getTop(); row <= reg.getBottom(); row++) {
            for (int column = reg.getLeft(); column <= reg.getRight(); column++) {
                if (column != reg.getLeft() || row != reg.getTop())
                    clearCellValue(column, row);
            }
        }
        setCellValue(reg.getLeft(), reg.getTop(), topLeftCellValue);
        mergedRegionsPool.add(reg);
        return sheet.addMergedRegion(new CellRangeAddress(reg.getTop(), reg.getBottom(), reg.getLeft(), reg.getRight()));
    }

    private Object findFirstValueInRegion(IGridRegion reg) {
        for (int row = reg.getTop(); row <= reg.getBottom(); row++) {
            for (int column = reg.getLeft(); column <= reg.getRight(); column++) {
                Object cellValue = getCell(column, row).getObjectValue();
                if (cellValue != null) {
                    return cellValue;
                }
            }
        }
        return null;
    }

    private CellRangeAddress getMergedRegionAt(int index) {
        return sheet.getMergedRegion(index);
    }

    public void beforeSave(XlsWorkbookSourceCodeModule xwscm) {
    }

    public void clearCellValue(int col, int row) {
        setCellValue(col, row, null);
    }

    public void clearCell(int col, int row) {
        setCellMetaInfo(col, row, null);
        Cell cell = getPoiXlsCell(col, row);
        if (cell != null) {
            sheet.getRow(row).removeCell(cell);
        }
    }

    public void copyCell(int colFrom, int rowFrom, int colTo, int rowTo) {
        Cell cellFrom = getPoiXlsCell(colFrom, rowFrom);
        copyCell(cellFrom, colTo, rowTo, getCellMetaInfo(colFrom, rowFrom));
    }

    // TODO To Refactor
    public void copyCell(Cell cellFrom, int colTo, int rowTo, CellMetaInfo meta) {
        Cell cellTo = getPoiXlsCell(colTo, rowTo);

        if (cellFrom == null) {
            if (cellTo != null) {
                clearCell(colTo, rowTo);
            }
            return;
        }
        if (cellTo == null) {
            cellTo = getOrCreatePoiXlsCell(colTo, rowTo);
        }

        copyCellValue(cellFrom, cellTo);
        copyCellStyle(cellFrom, cellTo);

        setCellMetaInfo(colTo, rowTo, meta);
    }

    public boolean isInOneMergedRegion(int firstCellColumn, int firstCellRow, int secondCellColumn, int secondCellRow) {
        IGridRegion region = mergedRegionsPool.getRegionContaining(firstCellColumn, firstCellRow);
        if (region != null && org.openl.rules.table.IGridRegion.Tool.contains(region, secondCellColumn, secondCellRow)) {
            return true;
        }
        return false;
    }

    public boolean isTopLeftCellInMergedRegion(int column, int row) {
        return super.getRegionStartingAt(column, row) != null;
    }

    public void copyCellValue(Cell cellFrom, Cell cellTo) {
        cellTo.setCellType(Cell.CELL_TYPE_BLANK);
        switch (cellFrom.getCellType()) {
            case Cell.CELL_TYPE_BLANK:
                break;
            case Cell.CELL_TYPE_BOOLEAN:
                cellTo.setCellValue(cellFrom.getBooleanCellValue());
                break;
            case Cell.CELL_TYPE_FORMULA:
                cellTo.setCellFormula(cellFrom.getCellFormula());
                break;
            case Cell.CELL_TYPE_NUMERIC:
                cellTo.setCellValue(cellFrom.getNumericCellValue());
                break;
            case Cell.CELL_TYPE_STRING:
                cellTo.setCellValue(cellFrom.getStringCellValue());
                break;
            default:
                throw new RuntimeException("Unknown cell type: " + cellFrom.getCellType());
        }
    }

    public void copyCellStyle(Cell cellFrom, Cell cellTo) {
        CellStyle styleFrom = cellFrom.getCellStyle();
        try {
            cellTo.setCellStyle(styleFrom);
        } catch (IllegalArgumentException e) { // copy cell style to cell of
                                               // another workbook
            CellStyle styleTo = sheet.getWorkbook().createCellStyle();
            styleTo.cloneStyleFrom(styleFrom);
            cellTo.setCellStyle(styleTo);
        }
    }

    /**
     * Copies properties of <code>ICellStyle</code> object to POI xls styling
     * object. <br/>
     * 
     * @param source style source
     * @param dest xls cell style object to fill
     */
    private void styleToXls(ICellStyle source, CellStyle dest) {
        if (source != null && dest != null) {
            dest.setAlignment((short) source.getHorizontalAlignment());
            dest.setVerticalAlignment((short) source.getVerticalAlignment());
            dest.setIndention((short) source.getIdent());

            short[] bs = source.getBorderStyle();
            dest.setBorderTop(bs[0]);
            dest.setBorderRight(bs[1]);
            dest.setBorderBottom(bs[2]);
            dest.setBorderLeft(bs[3]);
        }
    }

    public IGridRegion findEmptyRect(int width, int height) {
        int lastRow = sheet.getLastRowNum();
        int top = lastRow + 2, left = 1;

        return new GridRegion(top, left, top + height - 1, left + width - 1);
    }
    
    /**
     * Deprecated due to incorrect name. It was not clear that we get the cell from Poi.<br>
     * Use {@link #getPoiXlsCell(int, int)}
     *      
     */
    @Deprecated
    public Cell getXlsCell(int colIndex, int rowIndex) {
        return getPoiXlsCell(colIndex, rowIndex);
    }
    
    public Cell getPoiXlsCell(int colIndex, int rowIndex) {
        Row row = sheet.getRow(rowIndex);
        if (row != null) {
            return row.getCell(colIndex);
        }
        return null;
    }
    
    /**
     * Deprecated due to incorrect name. It was not clear that we get the cell from Poi.<br>
     * Use {@link #getOrCreatePoiXlsCell(int, int)}.
     */
    @Deprecated
    public Cell getOrCreateXlsCell(int colIndex, int rowIndex) {
        return getOrCreatePoiXlsCell(colIndex, rowIndex);
    }
    
    public Cell getOrCreatePoiXlsCell(int colIndex, int rowIndex) {
        Row row = sheet.getRow(rowIndex);
        if (row == null) {
            row = sheet.createRow(rowIndex);
        }
        Cell cell = row.getCell(colIndex);
        if (cell == null) {
            cell = row.createCell(colIndex);
        }
        return cell;
    }
    
    // TODO: we need to cache cell values.
    public XlsCell getCell(int column, int row) {        
        return new XlsCell(column, row, this);
    }

    // protected to be accessible from XlsCell
    protected CellMetaInfo getCellMetaInfo(int col, int row) {
        CellKey ck = new CellKey(col, row);
        return metaInfoMap.get(ck);
    }

    /**
     * Some magic numbers here What is column width???
     */
    public int getColumnWidth(int col) {
        int w = sheet.getColumnWidth((short) col);
        if (w == sheet.getDefaultColumnWidth()) {
            return 79;
        }
        return w / 40;
    }

    /**
     * Returns the index of the column. After that column there is no more
     * filled cells on the sheet in given row.
     * 
     * @param rownum index of the row on the sheet
     * @return
     */
    public int getMaxColumnIndex(int rownum) {
        Row row = sheet.getRow(rownum);
        return row == null ? 0 : row.getLastCellNum();
    }

    public int getMaxRowIndex() {
        return sheet.getLastRowNum();
    }

    public synchronized IGridRegion getMergedRegion(int i) {
        return new XlsGridRegion(getMergedRegionAt(i));
    }

    /**
     * Returns the index of the column, the next column will be the first cell
     * with data in given row.
     * 
     */
    public int getMinColumnIndex(int rownum) {
        Row row = sheet.getRow(rownum);
        return row == null ? 0 : row.getFirstCellNum();
    }

    public int getMinRowIndex() {
        return sheet.getFirstRowNum();
    }

    public String getName() {
        return sheetSource.getSheetName();
    }

    public int getNumberOfMergedRegions() {
        try {
            return sheet.getNumMergedRegions();
        } catch (NullPointerException e) {
            return 0;
        }
    }

    /**
     * Gets the URI to the table by its four coordinates on the sheet.
     * 
     * @return URI to the table in the sheet. (e.g.
     *         <code>file:D:\work\Workspace\org.openl.tablets.tutorial4\rules
     * \main&wbName=Tutorial_4.xls&wsName=Vehicle-Scoring&range=B3:D12</code>)
     */
    public String getRangeUri(int colStart, int rowStart, int colEnd, int rowEnd) {

        if (colStart == colEnd && rowStart == rowEnd) {
            return getUri() + "&" + "cell=" + getCell(colStart, rowStart).getUri();
        }

        return getUri() + "&" + "range=" + getCell(colStart, rowStart).getUri() + RANGE_SEPARATOR + getCell(colEnd,
            rowEnd).getUri();
    }

    /**
     * Gets the URI to the table by table region. Just calls
     * {@link XlsSheetGridModel#getRangeUri(int, int, int, int)}.
     * 
     * @param region Table region.
     * @return URI to the table in the sheet. (e.g.
     *         <code>file:D:\work\Workspace\org.openl.tablets.tutorial4\rules
     * \main&wbName=Tutorial_4.xls&wsName=Vehicle-Scoring&range=B3:D12</code>)
     */
    public String getRangeUri(IGridRegion region) {
        return getRangeUri(region.getLeft(), region.getTop(), region.getRight(), region.getBottom());
    }

    public IGridRegion getRegionContaining(int x, int y) {
        return mergedRegionsPool.getRegionContaining(x, y);
    }

    /**
     * Deprecated as there is no usages
     */
    @Deprecated
    public static boolean contains(CellRangeAddress reg, int x, int y) {
        return reg.getFirstColumn() <= x && x <= reg.getLastColumn() && reg.getFirstRow() <= y && y <= reg.getLastRow();
    }

    public XlsSheetSourceCodeModule getSheetSource() {
        return sheetSource;
    }

    public String getUri() {
        String xlsUri = sheetSource == null ? "" : sheetSource.getUri(0);
        return xlsUri;// + "#" + name;

    }

    public boolean isEmpty(int x, int y) {
        Cell cell = getPoiXlsCell(x, y);
        if (cell == null) {
            return true;
        }

        if (cell.getCellType() == Cell.CELL_TYPE_BLANK) {
            return true;
        }

        if (cell.getCellType() == Cell.CELL_TYPE_STRING) {

            String v = getCell(x, y).getStringValue();
            return v == null || v.trim().length() == 0;
        }
        return false;

    }

    public void removeMergedRegion(IGridRegion remove) {
        removeMergedRegion(remove.getLeft(), remove.getTop());
    }

    public void removeMergedRegion(int x, int y) {
        mergedRegionsPool.remove(x, y);
        int nregions = getNumberOfMergedRegions();
        for (int i = 0; i < nregions; i++) {
            CellRangeAddress reg = getMergedRegionAt(i);
            if (reg.getFirstColumn() == x && reg.getFirstRow() == y) {
                sheet.removeMergedRegion(i);
                if (sheet instanceof XSSFSheet && sheet.getNumMergedRegions() == 0) {
                    // TODO remove this when in will be implemented in POI(see
                    // https://issues.apache.org/bugzilla/show_bug.cgi?id=49895)
                    ((XSSFSheet) sheet).getCTWorksheet().unsetMergeCells();
                }
                return;
            }
        }
    }

    public void setCellMetaInfo(int col, int row, CellMetaInfo meta) {
        CellKey ck = new CellKey(col, row);
        if (meta == null) {
            metaInfoMap.remove(ck);
        } else {
            metaInfoMap.put(ck, meta);
        }
    }

    public void setCellStringValue(int col, int row, String value) {
        Cell cell = getOrCreatePoiXlsCell(col, row);
        cell.setCellValue(value);
    }

    public void setCellStyle(int col, int row, ICellStyle style) {
        Cell xlsCell = getOrCreatePoiXlsCell(col, row);
        CellStyle currentXlsStyle = xlsCell.getCellStyle();
        CellStyle newXlsStyle = sheet.getWorkbook().createCellStyle();
        newXlsStyle.cloneStyleFrom(currentXlsStyle);
        styleToXls(style, newXlsStyle); // apply our style changes
        xlsCell.setCellStyle(newXlsStyle);
    }

    public void setCellValue(int col, int row, Object value) {
        Cell xlsCell = getOrCreatePoiXlsCell(col, row);
        if (value != null) {
            boolean writeCellMetaInfo = true;

            // Don't write meta info for predefined String arrays to avoid
            // removing Enum Domain meta info.
            if (hasPredefinedStringArray(col, row)) {
                writeCellMetaInfo = false;
            }

            AXlsCellWriter cellWriter = getCellWriter(xlsCell, value);
            cellWriter.setCellToWrite(xlsCell);
            cellWriter.setValueToWrite(value);
            cellWriter.writeCellValue(writeCellMetaInfo);
        } else {
            xlsCell.setCellType(CELL_TYPE_BLANK);
        }
    }

    /**
     * @deprecated
     */
    private boolean hasPredefinedStringArray(int col, int row) {
        boolean result = false;

        ICell cell = getCell(col, row);
        if (cell != null) {
            CellMetaInfo cellMetaInfo = cell.getMetaInfo();
            IOpenClass dataType = cellMetaInfo == null ? null : cellMetaInfo.getDataType();
            if (dataType != null) {
                IDomain<?> domain = dataType.getDomain();
                Class<?> instanceClass = dataType.getInstanceClass();
                if (instanceClass == String.class && domain instanceof EnumDomain<?>) {
                    result = true;
                }
            }
        }
        return result;
    }

    // TODO: move to factory.
    private AXlsCellWriter getCellWriter(Cell cell, Object value) {
        String strValue = String.valueOf(value);
        AXlsCellWriter result = null;
        if (value instanceof Number) {
            result = cellWriters.get(AXlsCellWriter.NUMBER_WRITER);
        } else if (value instanceof Date) {
            result = cellWriters.get(AXlsCellWriter.DATE_WRITER);
        } else if (value instanceof Boolean) {
            result = cellWriters.get(AXlsCellWriter.BOOLEAN_WRITER);
        } else if (EnumUtils.isEnum(value)) {
            result = cellWriters.get(AXlsCellWriter.ENUM_WRITER);
        } else if (EnumUtils.isEnumArray(value)) {
            result = cellWriters.get(AXlsCellWriter.ENUM_ARRAY_WRITER);
        } else if (value.getClass().isArray()) {
            result = cellWriters.get(AXlsCellWriter.ARRAY_WRITER);
        } else { // String
            // Formula
            if (strValue.startsWith("=") && cell.getCellType() == Cell.CELL_TYPE_FORMULA) {
                result = cellWriters.get(AXlsCellWriter.FORMULA_WRITER);
            } else {
                result = cellWriters.get(AXlsCellWriter.STRING_WRITER);
            }
        }
        return result;
    }

}
