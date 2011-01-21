/*
 * Created on Sep 19, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.table.xls;

import java.awt.Color;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.poi.hssf.record.PaletteRecord;
import org.apache.poi.hssf.usermodel.HSSFPalette;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
//import org.apache.poi.ss.usermodel.Comment;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellUtil;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.openl.domain.EnumDomain;
import org.openl.domain.IDomain;
import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule;
import org.openl.rules.lang.xls.types.CellMetaInfo;
import org.openl.rules.table.AGrid;
import org.openl.rules.table.CellKey;
import org.openl.rules.table.GridRegion;
import org.openl.rules.table.ICell;
import org.openl.rules.table.ICellComment;
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

/**
 * @author snshor
 * 
 */
public class XlsSheetGridModel extends AGrid implements IWritableGrid, XlsWorkbookSourceCodeModule.WorkbookListener {

    private XlsSheetSourceCodeModule sheetSource;

    private Sheet sheet;
    private RegionsPool mergedRegionsPool;

    private Map<CellKey, CellMetaInfo> metaInfoMap = new HashMap<CellKey, CellMetaInfo>();

    private Map<String, AXlsCellWriter> cellWriters = new HashMap<String, AXlsCellWriter>();

    // private final static Log LOG =
    // LogFactory.getLog(XlsSheetGridModel.class);
    
    /**
     * Use {@link IGridRegion.Tool#getColumn(String)}
     */
    @Deprecated
    public static int getColumn(String cell) {
        return IGridRegion.Tool.getColumn(cell);
    }
    
    /**
     * Use {@link IGridRegion.Tool#getRow(String)}
     */
    @Deprecated
    public static int getRow(String cell) {
        return IGridRegion.Tool.getRow(cell);
    }
    
    /**
     * Use {@link IGridRegion.Tool#makeRegion(String)}
     */
    @Deprecated
    public static IGridRegion makeRegion(String range) {
        return IGridRegion.Tool.makeRegion(range);
    }
        
    /**
     * For internal usages, when we need to create virtual grid.
     * 
     * @deprecated For creating virtual grids use {@link XlsSheetGridHelper#createVirtualGrid(Sheet)}
     */
    protected XlsSheetGridModel(Sheet sheet) {
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
            CellRangeAddress reg = PoiExcelHelper.getMergedRegionAt(i, sheet);
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

    public void beforeSave(XlsWorkbookSourceCodeModule xwscm) {
    }

    public void clearCellValue(int col, int row) {
        setCellValue(col, row, null);
    }

    public void clearCell(int col, int row) {
        setCellMetaInfo(col, row, null);
        Cell cell = PoiExcelHelper.getCell(col, row, sheet);
        if (cell != null) {
            sheet.getRow(row).removeCell(cell);
        }
    }

    public void copyCell(int colFrom, int rowFrom, int colTo, int rowTo) {
        Cell cellFrom = PoiExcelHelper.getCell(colFrom, rowFrom, sheet);
        copyCell(cellFrom, colTo, rowTo, getCellMetaInfo(colFrom, rowFrom));
    }

    public void createCell(int col, int row, Object value, String formula, ICellStyle style, ICellComment comment) {
        if (StringUtils.isNotBlank(formula)) {
            setCellFormula(col, row, formula);
        } else {
            setCellValue(col, row, value);
        }
        setCellStyle(col, row, style);
        setCellMetaInfo(col, row, getCellMetaInfo(col, row));
        setCellComment(col, row, comment);
    }

    protected void copyCell(Cell cellFrom, int colTo, int rowTo, CellMetaInfo meta) {
        Cell cellTo = PoiExcelHelper.getCell(colTo, rowTo, sheet);

        if (cellFrom == null) {
            if (cellTo != null) {
                clearCell(colTo, rowTo);
            }
            return;
        }
        if (cellTo == null) {
            cellTo = PoiExcelHelper.getOrCreateCell(colTo, rowTo, sheet);
        }

        PoiExcelHelper.copyCellValue(cellFrom, cellTo);
        PoiExcelHelper.copyCellStyle(cellFrom, cellTo, sheet);
        //PoiExcelHelper.copyCellComment(cellFrom, cellTo);

        setCellMetaInfo(colTo, rowTo, meta);
    }

    public IGridRegion findEmptyRect(int width, int height) {        
        int lastRow = PoiExcelHelper.getLastRowNum(sheet);
        int top = lastRow + 2, left = 1;

        return new GridRegion(top, left, top + height - 1, left + width - 1);
    }
    
    /**
     * Deprecated due to incorrect name. It was not clear that we get the cell from Poi.<br>
     * Use {@link PoiExcelHelper#getCell(int, int)}
     *      
     */
    @Deprecated
    public Cell getXlsCell(int colIndex, int rowIndex) {
        return PoiExcelHelper.getCell(colIndex, rowIndex, sheet);
    }
    
    /**
     * Deprecated due to incorrect name. It was not clear that we get the cell from Poi.<br>
     * Use {@link PoiExcelHelper#getOrCreateCell(int, int)}.
     */
    @Deprecated
    public Cell getOrCreateXlsCell(int colIndex, int rowIndex) {
        return PoiExcelHelper.getOrCreateCell(colIndex, rowIndex, sheet);
    }

    public ICell getCell(int column, int row) {        
        return new XlsCell(column, row, this);
    }

    // protected to be accessible from XlsCell
    protected CellMetaInfo getCellMetaInfo(int col, int row) {
        CellKey ck = new CellKey(col, row);
        return metaInfoMap.get(ck);
    }

    public int getColumnWidth(int col) {
        return PoiExcelHelper.getColumnWidth(col, sheet);
    }
    
    public int getMaxColumnIndex(int rownum) {
        return PoiExcelHelper.getMaxColumnIndex(rownum, sheet);
    }

    public int getMaxRowIndex() {
        return PoiExcelHelper.getMaxRowIndex(sheet);
    }

    public synchronized IGridRegion getMergedRegion(int i) {
        return new XlsGridRegion(PoiExcelHelper.getMergedRegionAt(i, sheet));
    }

    public int getMinColumnIndex(int rownum) {
        return PoiExcelHelper.getMinColumnIndex(rownum, sheet);
    }

    public int getMinRowIndex() {
        return PoiExcelHelper.getMinRowIndex(sheet);
    }

    public String getName() {
        return sheetSource.getSheetName();
    }

    public int getNumberOfMergedRegions() {
        return PoiExcelHelper.getNumberOfMergedRegions(sheet);
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
        return PoiExcelHelper.isEmptyCell(x, y, sheet);
    }

    public void removeMergedRegion(IGridRegion remove) {
        removeMergedRegion(remove.getLeft(), remove.getTop());
    }

    public void removeMergedRegion(int x, int y) {
        mergedRegionsPool.remove(x, y);
        int nregions = getNumberOfMergedRegions();
        for (int i = 0; i < nregions; i++) {
            CellRangeAddress reg = PoiExcelHelper.getMergedRegionAt(i, sheet);
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

    public void setCellValue(int col, int row, Object value) {
        Cell poiCell = PoiExcelHelper.getOrCreateCell(col, row, sheet);
        if (value != null) {
            boolean writeCellMetaInfo = true;

            // Don't write meta info for predefined String arrays to avoid
            // removing Enum Domain meta info.
            if (hasPredefinedStringArray(col, row)) {
                writeCellMetaInfo = false;
            }

            AXlsCellWriter cellWriter = getCellWriter(poiCell.getCellType(), value);
            cellWriter.setCellToWrite(poiCell);
            cellWriter.setValueToWrite(value);
            cellWriter.writeCellValue(writeCellMetaInfo);
        } else {
            poiCell.setCellType(CELL_TYPE_BLANK);
        }
    }

    public void setCellStringValue(int col, int row, String value) {
        PoiExcelHelper.setCellStringValue(col, row, value, sheet);
    }

    public void setCellFormula(int col, int row, String formula) {
        Cell poiCell = PoiExcelHelper.getOrCreateCell(col, row, sheet);

        if (formula != null) {
            AXlsCellWriter cellWriter = cellWriters.get(AXlsCellWriter.FORMULA_WRITER);
            cellWriter.setCellToWrite(poiCell);
            cellWriter.setValueToWrite(formula);
            cellWriter.writeCellValue(false);
        }
    }

    public void setCellStyle(int col, int row, ICellStyle style) {
        Cell poiCell = PoiExcelHelper.getOrCreateCell(col, row, sheet);
        CellStyle newPoiStyle = sheet.getWorkbook().createCellStyle();

        CellStyle styleToClone = null;

        if (style instanceof XlsCellStyle) {
            styleToClone = ((XlsCellStyle) style).getXlsStyle();
        } else if (style instanceof XlsCellStyle2) {
            styleToClone = ((XlsCellStyle2) style).getXlsStyle();
        } else {
            styleToClone = poiCell.getCellStyle();
        }

        newPoiStyle.cloneStyleFrom(styleToClone);

        poiCell.setCellStyle(newPoiStyle);
    }

    public void setCellAlignment(int col, int row, int alignment) {
        Cell cell = PoiExcelHelper.getOrCreateCell(col, row, sheet);
        CellUtil.setCellStyleProperty(cell, sheet.getWorkbook(), CellUtil.ALIGNMENT, (short) alignment);
    }

    public void setCellIndent(int col, int row, int indent) {
        Cell cell = PoiExcelHelper.getOrCreateCell(col, row, sheet);
        CellUtil.setCellStyleProperty(cell, sheet.getWorkbook(), CellUtil.INDENTION, (short) indent);
    }

    public void setCellFillColor(int col, int row, short[] color) {
        Cell cell = PoiExcelHelper.getOrCreateCell(col, row, sheet);
        CellStyle newStyle = PoiExcelHelper.cloneStyleFrom(cell);

        if (color != null) {
            if (newStyle.getFillPattern() == CellStyle.NO_FILL) {
                newStyle.setFillPattern(CellStyle.SOLID_FOREGROUND);
            }
            setCellFillColor(newStyle, color);    
        } else {
            newStyle.setFillPattern(CellStyle.NO_FILL);
        }

        cell.setCellStyle(newStyle);
    }

    private void setCellFillColor(CellStyle dest, short[] rgb) {
        // Xlsx
        if (dest instanceof XSSFCellStyle) {
            XSSFColor color = new XSSFColor(new Color(rgb[0], rgb[1], rgb[2]));
            ((XSSFCellStyle) dest).setFillForegroundColor(color);

        // Xls
        } else { 
            HSSFColor color = findIndexedColor(rgb);
            if (color != null) {
                dest.setFillForegroundColor(color.getIndex());
            }
        }
    }

    public void setCellFontColor(int col, int row, short[] color) {
        Cell cell = PoiExcelHelper.getOrCreateCell(col, row, sheet);

        CellStyle newStyle = PoiExcelHelper.cloneStyleFrom(cell);
        Font newFont = PoiExcelHelper.cloneFontFrom(cell);

        if (color != null) {
            setCellFontColor(newFont, color);
        } else {
            newFont.setColor(HSSFColor.BLACK.index);
        }

        newStyle.setFont(newFont);
        cell.setCellStyle(newStyle);
    }

    private void setCellFontColor(Font dest, short[] rgb) {
        // Xlsx
        if (dest instanceof XSSFFont) {
            XSSFColor color = new XSSFColor(new Color(rgb[0], rgb[1], rgb[2]));
            ((XSSFFont) dest).setColor(color);

        // Xls
        } else {
            HSSFColor color = findIndexedColor(rgb);
            if (color != null) {
                dest.setColor(color.getIndex());
            }
        }
    }

    private HSSFColor findIndexedColor(short[] rgb) {
        HSSFPalette palette = ((HSSFWorkbook) sheet.getWorkbook()).getCustomPalette();
        HSSFColor color = palette.findColor((byte) rgb[0], (byte) rgb[1], (byte) rgb[2]);

        if (color == null) {
            Set<Short> usedColors = sheetSource.getWorkbookSource().getWorkbookColors();

            short fromIndex = PaletteRecord.FIRST_COLOR_INDEX;
            short toIndex = (short) (PaletteRecord.STANDARD_PALETTE_SIZE + fromIndex);
            for (short colorIndex = fromIndex; colorIndex < toIndex; colorIndex++) {
                if (!usedColors.contains(colorIndex)) {
                    palette.setColorAtIndex(colorIndex, (byte) rgb[0], (byte) rgb[1], (byte) rgb[2]);
                    color = palette.getColor(colorIndex);
                    usedColors.add(colorIndex);
                    break;
                }
            }
            if (color == null) {
                color = palette.findSimilarColor((byte) rgb[0], (byte) rgb[1], (byte) rgb[2]);
            }
        }
        return color;
    }

    public void setCellFontBold(int col, int row, boolean bold) {
        Cell cell = PoiExcelHelper.getOrCreateCell(col, row, sheet);
        short boldweight = bold ? Font.BOLDWEIGHT_BOLD : Font.BOLDWEIGHT_NORMAL;
        PoiExcelHelper.setCellFontBold(cell, boldweight);
    }

    public void setCellFontItalic(int col, int row, boolean italic) {
        Cell cell = PoiExcelHelper.getOrCreateCell(col, row, sheet);
        PoiExcelHelper.setCellFontItalic(cell, italic);
    }

    public void setCellFontUnderline(int col, int row, boolean underlined) {
        Cell cell = PoiExcelHelper.getOrCreateCell(col, row, sheet);
        byte underline = underlined ? Font.U_SINGLE : Font.U_NONE;
        PoiExcelHelper.setCellFontUnderline(cell, underline);
    }

    public void setCellComment(int col, int row, ICellComment comment) {
        // setCellComment method of POI 3.7 corrupts workbook
        // TODO Uncomment when POI team will fix this issue

        /*Comment poiComment = null;
        Cell poiCell = PoiExcelHelper.getOrCreateCell(col, row, sheet);

        if (comment != null) {
            poiComment = ((XlsCellComment) comment).getXlxComment();
        }
        poiCell.setCellComment(poiComment);*/
    }

    /**
     * @deprecated
     */
    public boolean hasPredefinedStringArray(int col, int row) {
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
    public AXlsCellWriter getCellWriter(int cellType, Object value) {
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
            String strValue = String.valueOf(value);
            // Formula
            if (strValue.startsWith("=") && cellType == CELL_TYPE_FORMULA) {
                result = cellWriters.get(AXlsCellWriter.FORMULA_WRITER);
            } else {
                result = cellWriters.get(AXlsCellWriter.STRING_WRITER);
            }
        }
        return result;
    }

}
