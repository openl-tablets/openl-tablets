/*
 * Created on Sep 19, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.table.xls;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule;
import org.openl.rules.lang.xls.types.CellMetaInfo;
import org.openl.rules.table.AGridModel;
import org.openl.rules.table.CellKey;
import org.openl.rules.table.GridRegion;
import org.openl.rules.table.ICell;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IWritableGrid;
import org.openl.rules.table.ui.ICellFont;
import org.openl.rules.table.ui.ICellStyle;
import org.openl.util.StringTool;

/**
 * @author snshor
 * 
 */
public class XlsSheetGridModel extends AGridModel implements IWritableGrid,
        XlsWorkbookSourceCodeModule.WorkbookListener {

    class XlsCell implements ICell {

        private int column;

        private int row;

        private IGridRegion region;

        private Cell cell;

        public XlsCell(int column, int row, IGridRegion region, Cell cell) {
            this.column = column;
            this.row = row;
            this.region = region;
            this.cell = cell;
        }

        public ICellStyle getStyle() {
            if (cell == null) return null;
            return getCellStyle(column, row, cell);
        }

        public int getColumn() {
            return column;
        }

        public ICellFont getFont() {
            if (cell == null) return null;
            Font font = sheetSource.getWorkbookSource().getWorkbook().getFontAt(cell.getCellStyle().getFontIndex());
            return new XlsCellFont(font, sheetSource.getWorkbookSource().getWorkbook());
        }

        public int getRow() {
            return row;
        }

        public IGridRegion getRegion() {
            return region;
        }

        public int getHeight() {
            if (region != null && region.getLeft() == column && region.getTop() == row) {
                return region.getBottom() - region.getTop() + 1;
            }
            return 1;
        }

        public int getWidth() {
            if (region != null && region.getLeft() == column && region.getTop() == row) {
                return region.getRight() - region.getLeft() + 1;
            }
            return 1;
        }

        public Object getObjectValue() {
            if (cell == null) return null;
            int valueType = cell.getCellType();
            switch (valueType) {
                case Cell.CELL_TYPE_BLANK:
                    return null;
                case Cell.CELL_TYPE_BOOLEAN:
                    return Boolean.valueOf(cell.getBooleanCellValue());
                case Cell.CELL_TYPE_NUMERIC:
                    if (DateUtil.isCellDateFormatted(cell)) {
                        return cell.getDateCellValue();
                    }
                    double value = cell.getNumericCellValue();
                    return XlsUtil.intOrDouble(value);
                case Cell.CELL_TYPE_STRING:
                    return cell.getStringCellValue();
                case Cell.CELL_TYPE_FORMULA:
                    FormulaEvaluator formulaEvaluator = sheet.getWorkbook().getCreationHelper().createFormulaEvaluator();
                    CellValue resultValue = formulaEvaluator.evaluate(cell);
                    return XlsUtil.intOrDouble(resultValue.getNumberValue());
                default:
                    return "unknown type: " + cell.getCellType();
            }
        }

        public String getStringValue() {
            Object res = getObjectValue();
            return res == null ? null : String.valueOf(res);
        }

        public String getFormula() {
            return cell.getCellType() == CELL_TYPE_FORMULA ? cell.getCellFormula() : null;
        }

        public int getType() {
            if (cell == null) return Cell.CELL_TYPE_BLANK;
            return cell.getCellType();
        }

        public String getUri() {
            return XlsUtil.xlsCellPresentation(column, row);
        }

    }

    public static final String RANGE_SEPARATOR = ":";

    XlsSheetSourceCodeModule sheetSource;

    Sheet sheet;

    Map<CellKey, CellMetaInfo> metaInfoMap = new HashMap<CellKey, CellMetaInfo>();

    /**
     * Not saved styles for cells.
     */
    private Map<CellKey, ICellStyle> styleMap = new HashMap<CellKey, ICellStyle>();

    static public int getColumn(String cell) {
        int col = 0;
        int mul = 'Z' - 'A' + 1;
        for (int i = 0; i < cell.length(); i++) {
            char ch = cell.charAt(i);
            if (!Character.isLetter(ch)) {
                return col;
            }
            col = col * mul + ch - 'A';
        }
        throw new RuntimeException("Invalid cell: " + cell);
    }

    static public int getRow(String cell) {
        for (int i = 0; i < cell.length(); i++) {
            char ch = cell.charAt(i);
            if (Character.isDigit(ch)) {
                return Integer.parseInt(cell.substring(i)) - 1;
            }
        }
        throw new RuntimeException("Invalid cell: " + cell);
    }

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
    }

    public XlsSheetGridModel(XlsSheetSourceCodeModule sheetSource) {
        this.sheetSource = sheetSource;
        sheet = sheetSource.getSheet();

        sheetSource.getWorkbookSource().addListener(this);
    }

    public int addMergedRegion(IGridRegion reg) {
        return sheet
                .addMergedRegion(new CellRangeAddress(reg.getTop(), reg.getBottom(), reg.getLeft(), reg.getRight()));
    }

    private CellRangeAddress getMergedRegionAt(int index) {
        return sheet.getMergedRegion(index);
    }    

    public void beforeSave(XlsWorkbookSourceCodeModule xwscm) {
        Workbook workbook = xwscm.getWorkbook();
        for (CellKey ck : styleMap.keySet()) {
            Cell cell = getXlsCell(ck.getColumn(), ck.getRow());
            if (cell != null) {
                CellStyle cellStyle = workbook.createCellStyle();
                copyStyle(styleMap.get(ck), cellStyle, cell.getCellStyle());
                cell.setCellStyle(cellStyle);
            }
        }
        styleMap.clear();
    }

    // XlsGridRegion[] regions = null;

    public void clearCell(int col, int row) {

        setCellMetaInfo(col, row, null);
        Cell cell = getXlsCell(col, row);
        if (cell == null) {
            return;
        }

        sheet.getRow(row).removeCell(cell);
    }

    public void copyCell(int colFrom, int rowFrom, int colTo, int rowTo) {
        Cell cellFrom = getXlsCell(colFrom, rowFrom);

        copyFrom(cellFrom, colTo, rowTo, getCellMetaInfo(colFrom, rowFrom));
    }

    public void copyFrom(Cell cellFrom, int colTo, int rowTo, CellMetaInfo meta) {
        Cell cellTo = getXlsCell(colTo, rowTo);

        if (cellFrom == null) {
            if (cellTo == null) {
                return;
            }
            clearCell(colTo, rowTo);
            return;
        }

        if (cellTo == null) {
            cellTo = createNewCell(colTo, rowTo);
        }

        cellTo.setCellType(Cell.CELL_TYPE_BLANK);
        // cellTo.setCellType(cellFrom.getCellType());

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

        CellStyle styleFrom = cellFrom.getCellStyle();
        CellStyle styleTo = cellTo.getCellStyle();

        styleTo.cloneStyleFrom(styleFrom);

        setCellMetaInfo(colTo, rowTo, meta);
    }

    /**
     * Copies properties of <code>ICellStyle</code> object to POI xls styling
     * object. <br/> <b>Note:</b> for now ignores font and some properties, to
     * set those ones another POI xls styling object is used.
     * 
     * @param source style source
     * @param dest xls cell style object to fill
     * @param oldStyle xls style object - another style source for properties
     *            that ignored in <code>ICellStyle source</code> parameter
     */
    private void copyStyle(ICellStyle source, CellStyle dest, CellStyle oldStyle) {
        if (source != null) {
        dest.setAlignment((short) source.getHorizontalAlignment());
        dest.setVerticalAlignment((short) source.getVerticalAlignment());
        dest.setIndention((short) source.getIdent());

        short[] bs = source.getBorderStyle();
        dest.setBorderTop(bs[0]);
        dest.setBorderRight(bs[1]);
        dest.setBorderBottom(bs[2]);
        dest.setBorderLeft(bs[3]);
        }
        // TODO Can't we clone style like below?
        // dest.cloneStyleFrom(oldStyle);
        if (oldStyle != null) {
            dest.setBottomBorderColor(oldStyle.getBottomBorderColor());
            dest.setTopBorderColor(oldStyle.getTopBorderColor());
            dest.setRightBorderColor(oldStyle.getRightBorderColor());
            dest.setLeftBorderColor(oldStyle.getLeftBorderColor());
            dest.setDataFormat(oldStyle.getDataFormat());
            dest.setFillBackgroundColor(oldStyle.getFillBackgroundColor());
            dest.setFillForegroundColor(oldStyle.getFillForegroundColor());
            dest.setFillPattern(oldStyle.getFillPattern());
            dest.setFont(sheetSource.getWorkbookSource().getWorkbook().getFontAt(oldStyle.getFontIndex()));
            dest.setHidden(oldStyle.getHidden());
            dest.setLocked(oldStyle.getLocked());
            dest.setRotation(oldStyle.getRotation());
            dest.setWrapText(oldStyle.getWrapText());
        }
    }

    public Cell createNewCell(int colTo, int rowTo) {
        Row row = sheet.getRow(rowTo);
        if (row == null) {
            row = sheet.createRow(rowTo);
        }

        Cell cell = row.getCell(colTo);
        if (cell == null) {
            cell = row.createCell(colTo);
        }
        return cell;
    }

    public IGridRegion findEmptyRect(int width, int height) {
        int lastRow = sheet.getLastRowNum();
        int top = lastRow + 2, left = 1;

        return new GridRegion(top, left, top + height - 1, left + width - 1);
    }

    public Cell getXlsCell(int x, int y) {
        Row row = sheet.getRow(y);
        if (row == null) {
            return null;
        }

        return row.getCell(x);
    }

    public ICell getCell(int column, int row) {
        CellRangeAddress region = getRegionContaining(column, row);
        return new XlsCell(column, row, region == null ? null : new XlsGridRegion(region), getXlsCell(column, row));
    }

    public CellMetaInfo getCellMetaInfo(int col, int row) {
        CellKey ck = new CellKey(col, row);
        return metaInfoMap.get(ck);
    }

    private ICellStyle getCellStyle(int column, int row, Cell cell) {
        ICellStyle newStyle = getModifiedStyle(column, row);
        if (newStyle != null) {
            return newStyle;
        }

        CellStyle style = cell.getCellStyle();

        if (style == null) {
            return null;
        } else {
            Workbook workbook = sheetSource.getWorkbookSource().getWorkbook();
            if (style instanceof XSSFCellStyle) {
                return new XlsCellStyle2((XSSFCellStyle) style, (XSSFWorkbook) workbook);
            } else {
                return new XlsCellStyle((HSSFCellStyle) style, (HSSFWorkbook) workbook);
            }
        }
    }

   /**
     * Some magic numbers here
     */

    public int getColumnWidth(int col) {
        int w = sheet.getColumnWidth((short) col);
        if (w == sheet.getDefaultColumnWidth()) {
            return 79;
        }
        return w / 40;
    }

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

     public int getMinColumnIndex(int rownum) {
        Row row = sheet.getRow(rownum);
        return row == null ? 0 : row.getFirstCellNum();
    }

    public int getMinRowIndex() {
        return sheet.getFirstRowNum();
    }

    ICellStyle getModifiedStyle(int column, int row) {
        return styleMap.get(new CellKey(column, row));
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

    public String getRangeUri(int colStart, int rowStart, int colEnd, int rowEnd) {

        if (colStart == colEnd && rowStart == rowEnd) {
            return getUri() + "&" + "cell=" + getCell(colStart, rowStart).getUri();
        }

        return getUri() + "&" + "range=" + getCell(colStart, rowStart).getUri() + RANGE_SEPARATOR
                + getCell(colEnd, rowEnd).getUri();
    }

    public CellRangeAddress getRegionContaining(int x, int y) {
        int nregions = getNumberOfMergedRegions();
        for (int i = 0; i < nregions; i++) {
            CellRangeAddress reg = getMergedRegionAt(i);
            if (contains(reg,  x, y)) {
                return reg;
            }
        }
        return null;
    }

    
    static boolean contains(CellRangeAddress reg, int x, int y) {
        return reg.getFirstColumn() <= x
        && x <= reg.getLastColumn()
        && reg.getFirstRow() <= y 
        && y <= reg.getLastRow();
    }
    
    public XlsSheetSourceCodeModule getSheetSource() {
        return sheetSource;
    }

    public String getUri() {
        String xlsUri = sheetSource == null ? "" : sheetSource.getUri(0);
        return xlsUri;// + "#" + name;

    }

    public boolean isEmpty(int x, int y) {
        Row row = sheet.getRow(y);
        if (row == null) {
            return true;
        }

        Cell cell = row.getCell(x);
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
        int nregions = getNumberOfMergedRegions();
        for (int i = 0; i < nregions; i++) {
            CellRangeAddress reg = getMergedRegionAt(i);
            if (reg.getFirstColumn() == x && reg.getFirstRow() == y) {
                sheet.removeMergedRegion(i);
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
        Cell cell = createNewCell(col, row);
        cell.setCellValue(value);
    }

    public void setCellStyle(int col, int row, ICellStyle style) {
        CellKey key = new CellKey(col, row);
        if (style == null) {
            styleMap.remove(key);
        } else {
            createNewCell(col, row);
            styleMap.put(key, style);
        }
    }

    public void setCellValue(int col, int row, Object value) {
        if (value == null) {
            return;
        }
        Cell cell = createNewCell(col, row);
        if (value instanceof Number) {
            Number x = (Number) value;
            cell.setCellValue(x.doubleValue());
        } else if (value instanceof Date) {
            Date x = (Date) value;
            
            cell.setCellValue(x);
            CellStyle cellStyle = cell.getSheet().getWorkbook().createCellStyle();
            CreationHelper createHelper = cell.getSheet().getWorkbook().getCreationHelper();
            cellStyle.setDataFormat(createHelper.createDataFormat().getFormat(DATE_FORMAT));
            cell.setCellStyle(cellStyle);
        } else if (value instanceof Boolean){
            Boolean boolValue = (Boolean) value;
            cell.setCellValue(boolValue.booleanValue());
        } else {
            String strValue = String.valueOf(value);
            // formula
            if (strValue.startsWith("=")) {
                cell.setCellFormula(strValue.replaceFirst("=", ""));
            } else {
                cell.setCellValue(strValue);
            }
        }
    }

}
