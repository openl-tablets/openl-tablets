/*
 * Created on Sep 19, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.table.xls;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.BuiltinFormats;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
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
import org.openl.types.java.JavaOpenClass;
import org.openl.util.EnumUtils;
import org.openl.util.StringTool;

/**
 * @author snshor
 * 
 */
public class XlsSheetGridModel extends AGridModel implements IWritableGrid,
        XlsWorkbookSourceCodeModule.WorkbookListener {
    
   // private final static Log LOG = LogFactory.getLog(XlsSheetGridModel.class);

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
            Object value = extractCellValue(false);
            return value;
        }
        
        private Object extractCellValue(boolean useCachedValue){
            switch (useCachedValue ? cell.getCachedFormulaResultType() : cell.getCellType()) {
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
                    try {
                        FormulaEvaluator formulaEvaluator = sheet.getWorkbook().getCreationHelper().createFormulaEvaluator();
                        formulaEvaluator.evaluateFormulaCell(cell);
                    } catch (RuntimeException e) {
                    }
                    //extract new calculated value or previously cached value if calculation failed
                    return extractCellValue(true);
                default:
                    return "unknown type: " + cell.getCellType();
            }
        }

        public String getStringValue() {
            Object res = null;
            try {
                res = getObjectValue();
            } catch (IncorrectFormulaException ex) {
                //logged in getObjectValue() method.
            }
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

    static public int getColumn(String cell) {
        int col = 0;
        int mul = 'Z' - 'A' + 1;
        for (int i = 0; i < cell.length(); i++) {
            char ch = cell.charAt(i);
            if (!Character.isLetter(ch)) {
                return col-1;
            }
            col = col * mul + ch - 'A' + 1;
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
    }

    public void clearCell(int col, int row) {
        setCellMetaInfo(col, row, null);
        Cell cell = getXlsCell(col, row);
        if (cell != null) {
            sheet.getRow(row).removeCell(cell);
        }
    }

    public void copyCell(int colFrom, int rowFrom, int colTo, int rowTo) {
        Cell cellFrom = getXlsCell(colFrom, rowFrom);
        copyCell(cellFrom, colTo, rowTo, getCellMetaInfo(colFrom, rowFrom));
    }

    // TODO To Refactor
    public void copyCell(Cell cellFrom, int colTo, int rowTo, CellMetaInfo meta) {
        Cell cellTo = getXlsCell(colTo, rowTo);

        if (cellFrom == null) {
            if (cellTo != null) {
                clearCell(colTo, rowTo);
            }
            return;
        }
        if (isInOneMergedRegion(cellFrom.getColumnIndex(), cellFrom.getRowIndex(), colTo, rowTo)
                && isTopLeftCellInMergedRegion(colTo, rowTo)) {
            return;
        }

        if (cellTo == null) {
            cellTo = getOrCreateXlsCell(colTo, rowTo);
        }

        copyCellValue(cellFrom, cellTo);
        copyCellStyle(cellFrom, cellTo);

        setCellMetaInfo(colTo, rowTo, meta);
    }
    
    private boolean isInOneMergedRegion(int firstCellColumn, int firstCellRow, int secondCellColumn, int secondCellRow) {
        for (int i = 0; i < getNumberOfMergedRegions(); i++) {
            IGridRegion existingMergedRegion = getMergedRegion(i);
            if (org.openl.rules.table.IGridRegion.Tool.contains(existingMergedRegion, firstCellColumn, firstCellRow)
                    && org.openl.rules.table.IGridRegion.Tool.contains(existingMergedRegion, secondCellColumn, secondCellRow)) {
                return true;
            }
        }
        return false;
    }

    private boolean isTopLeftCellInMergedRegion(int column, int row) {
        for (int i = 0; i < getNumberOfMergedRegions(); i++) {
            IGridRegion existingMergedRegion = getMergedRegion(i);
                if(existingMergedRegion.getTop() == row && existingMergedRegion.getLeft() == column){
                    return true;
            }
        }
        return false;
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
        } catch (IllegalArgumentException e) { // copy cell style to cell of another workbook
            CellStyle styleTo = sheet.getWorkbook().createCellStyle();
            styleTo.cloneStyleFrom(styleFrom);
            cellTo.setCellStyle(styleTo);
        }
    }

    /**
     * Copies properties of <code>ICellStyle</code> object to POI xls styling object. <br/>
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

    public Cell getXlsCell(int colIndex, int rowIndex) {
        Row row = sheet.getRow(rowIndex);
        if (row != null) {
            return row.getCell(colIndex);
        }
        return null;
    }

    public Cell getOrCreateXlsCell(int colIndex, int rowIndex) {
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

    public ICell getCell(int column, int row) {
        CellRangeAddress region = getRegionContaining(column, row);
        return new XlsCell(column, row, region == null ? null : new XlsGridRegion(region), getXlsCell(column, row));
    }

    public CellMetaInfo getCellMetaInfo(int col, int row) {
        CellKey ck = new CellKey(col, row);
        return metaInfoMap.get(ck);
    }

    private ICellStyle getCellStyle(int column, int row, Cell cell) {
        CellStyle style = cell.getCellStyle();
        if (style != null) {
            Workbook workbook = sheet.getWorkbook();
            if (style instanceof XSSFCellStyle) {
                return new XlsCellStyle2((XSSFCellStyle) style, (XSSFWorkbook) workbook);
            } else {
                return new XlsCellStyle((HSSFCellStyle) style, (HSSFWorkbook) workbook);
            }
        }
        return null;
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
     * @return URI to the table in the sheet. 
     * (e.g. <code>file:D:\work\Workspace\org.openl.tablets.tutorial4\rules
     * \main&wbName=Tutorial_4.xls&wsName=Vehicle-Scoring&range=B3:D12</code>)
     */
    public String getRangeUri(int colStart, int rowStart, int colEnd, int rowEnd) {

        if (colStart == colEnd && rowStart == rowEnd) {
            return getUri() + "&" + "cell=" + getCell(colStart, rowStart).getUri();
        }

        return getUri() + "&" + "range=" + getCell(colStart, rowStart).getUri() + RANGE_SEPARATOR
                + getCell(colEnd, rowEnd).getUri();
    }
    
    /**
     * Gets the URI to the table by table region.
     * Just calls {@link XlsSheetGridModel#getRangeUri(int, int, int, int)}.
     * 
     * @param region Table region.     
     * @return URI to the table in the sheet.
     * (e.g. <code>file:D:\work\Workspace\org.openl.tablets.tutorial4\rules
     * \main&wbName=Tutorial_4.xls&wsName=Vehicle-Scoring&range=B3:D12</code>)
     */
    public String getRangeUri(IGridRegion region) {
        return getRangeUri(region.getLeft(), region.getTop(), region.getRight(), region.getBottom());
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
        Cell cell = getOrCreateXlsCell(col, row);
        cell.setCellValue(value);
    }

    public void setCellStyle(int col, int row, ICellStyle style) {
        Cell xlsCell = getOrCreateXlsCell(col, row);
        CellStyle currentXlsStyle = xlsCell.getCellStyle();
        CellStyle newXlsStyle = sheet.getWorkbook().createCellStyle();
        newXlsStyle.cloneStyleFrom(currentXlsStyle);
        styleToXls(style, newXlsStyle); // apply our style changes
        xlsCell.setCellStyle(newXlsStyle);
    }

    public void setCellValue(int col, int row, Object value) {
        
        if (value == null) {
            return;
        }
        
        Cell cell = getOrCreateXlsCell(col, row);
        String strValue = String.valueOf(value);
        if (value instanceof Number) {
            
            Number numberValue = (Number) value;
            cell.setCellValue(numberValue.doubleValue());
            
            // we need to set cell meta info for the cell, to open appropriate editor for it on UI.
            CellMetaInfo numberMeta = new CellMetaInfo(CellMetaInfo.Type.DT_DATA_CELL, strValue, 
                    JavaOpenClass.getOpenClass(numberValue.getClass()), false);            
            setCellMetaInfo(col, row, numberMeta);
            return;
        } else if (value instanceof Date) {

            Date dateValue = (Date) value;
            cell.setCellValue(dateValue);
            
            CellStyle previousStyle = cell.getCellStyle();
            cell.setCellStyle(sheet.getWorkbook().createCellStyle());
            cell.getCellStyle().cloneStyleFrom(previousStyle);
            cell.getCellStyle().setDataFormat((short) BuiltinFormats
                    .getBuiltinFormat(XlsDateFormat.DEFAULT_XLS_DATE_FORMAT));
            
            CellMetaInfo dateMeta = new CellMetaInfo(CellMetaInfo.Type.DT_DATA_CELL, strValue, 
                    JavaOpenClass.getOpenClass(dateValue.getClass()), false);            
            setCellMetaInfo(col, row, dateMeta);
            return;
        } else if (value instanceof Boolean) {
        
            Boolean boolValue = (Boolean) value;
            cell.setCellValue(boolValue.booleanValue());
            
            CellMetaInfo booleanMeta = new CellMetaInfo(CellMetaInfo.Type.DT_DATA_CELL, strValue, 
                    JavaOpenClass.getOpenClass(boolValue.getClass()), false);            
            setCellMetaInfo(col, row, booleanMeta);
            return;
        
        } else if (EnumUtils.isEnum(value)) {
            cell.setCellValue(((Enum<?>) value).name());
            
            CellMetaInfo enumMeta = new CellMetaInfo(CellMetaInfo.Type.DT_DATA_CELL, strValue, 
                    JavaOpenClass.getOpenClass(value.getClass()), false);            
            setCellMetaInfo(col, row, enumMeta);
            return;
        
        } else if (EnumUtils.isEnumArray(value)) {
        
            Object[] enums = (Object[]) value;
            String[] names = EnumUtils.getNames(enums);
            cell.setCellValue(StringUtils.join(names, ","));
            
            // we have an array of Enums. we need to set as meta info information that domain class is Enum, so we 
            // need to take a component class and multiValue to true as it is an array.
            CellMetaInfo enumArrayMeta = new CellMetaInfo(CellMetaInfo.Type.DT_DATA_CELL, strValue, 
                    JavaOpenClass.getOpenClass(value.getClass().getComponentType()), true);            
            setCellMetaInfo(col, row, enumArrayMeta);
            return;
     
        } else if (value.getClass().isArray()) {
            
            Object[] values = (Object[]) value;
            cell.setCellValue(StringUtils.join(values, ","));
            
            CellMetaInfo meta = new CellMetaInfo(CellMetaInfo.Type.DT_DATA_CELL, strValue, 
                    JavaOpenClass.getOpenClass(value.getClass().getComponentType()), false);            
            setCellMetaInfo(col, row, meta);
            return;

        } else { // String       
            
            // Formula
            if (strValue.startsWith("=") && cell.getCellType() == Cell.CELL_TYPE_FORMULA) {
                cell.setCellFormula(strValue.replaceFirst("=", ""));
            } else {
                cell.setCellType(Cell.CELL_TYPE_BLANK);
                cell.setCellValue(strValue);
                
                CellMetaInfo meta = new CellMetaInfo(CellMetaInfo.Type.DT_DATA_CELL, strValue, 
                        JavaOpenClass.getOpenClass(strValue.getClass()), false);            
                setCellMetaInfo(col, row, meta);
            }
        }
    }

}
