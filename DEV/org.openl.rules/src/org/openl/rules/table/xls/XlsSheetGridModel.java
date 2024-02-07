/*
 * Created on Sep 19, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.table.xls;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.poi.hssf.record.PaletteRecord;
import org.apache.poi.hssf.usermodel.HSSFPalette;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.Comment;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellUtil;
import org.apache.poi.xssf.usermodel.IndexedColorMap;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import org.openl.rules.lang.xls.SpreadsheetConstants;
import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.table.AGrid;
import org.openl.rules.table.GridRegion;
import org.openl.rules.table.ICell;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridRegion.Tool;
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
import org.openl.util.EnumUtils;
import org.openl.util.StringUtils;

/**
 * @author snshor
 */
public class XlsSheetGridModel extends AGrid implements IWritableGrid {

    private final XlsSheetSourceCodeModule sheetSource;

    private RegionsPool mergedRegionsPool;

    private final Map<String, AXlsCellWriter> cellWriters = new HashMap<>();

    public XlsSheetGridModel(XlsSheetSourceCodeModule sheetSource) {
        this.sheetSource = sheetSource;
    }

    private void extractMergedRegions() {
        mergedRegionsPool = new RegionsPool(null);
        int nregions = getNumberOfMergedRegions();
        Sheet sheet = getSheet();
        for (int i = 0; i < nregions; i++) {
            CellRangeAddress reg = sheet.getMergedRegion(i);
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

    @Override
    public int addMergedRegion(IGridRegion reg) {
        Object topLeftCellValue = findFirstValueInRegion(reg);
        for (int row = reg.getTop(); row <= reg.getBottom(); row++) {
            for (int column = reg.getLeft(); column <= reg.getRight(); column++) {
                if (column != reg.getLeft() || row != reg.getTop()) {
                    clearCellValue(column, row);
                }
            }
        }
        setCellValue(reg.getLeft(), reg.getTop(), topLeftCellValue);
        getMergedRegionsPool().add(reg);
        return getSheet()
                .addMergedRegion(new CellRangeAddress(reg.getTop(), reg.getBottom(), reg.getLeft(), reg.getRight()));
    }

    private RegionsPool getMergedRegionsPool() {
        if (mergedRegionsPool == null) {
            extractMergedRegions();
        }
        return mergedRegionsPool;
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

    public void clearCellValue(int col, int row) {
        setCellValue(col, row, null);
    }

    @Override
    public void clearCell(int col, int row) {
        Sheet sheet = getSheet();
        Cell cell = PoiExcelHelper.getCell(col, row, sheet);
        if (cell != null) {
            cell.removeCellComment();
            cell.removeHyperlink();
            sheet.getRow(row).removeCell(cell);
        }
    }

    @Override
    public void copyCell(int colFrom, int rowFrom, int colTo, int rowTo) {
        Cell cellFrom = PoiExcelHelper.getCell(colFrom, rowFrom, getSheet());
        Sheet sheet = getSheet();
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

        copyCellValue(cellFrom, cellTo);
        CellStyle styleFrom = cellFrom.getCellStyle();
        try {
            cellTo.setCellStyle(styleFrom);
        } catch (IllegalArgumentException e) { // copy cell style to cell of
            // another workbook
            CellStyle styleTo = PoiExcelHelper.createCellStyle(sheet.getWorkbook());
            styleTo.cloneStyleFrom(styleFrom);
            cellTo.setCellStyle(styleTo);
        }
        cellTo.removeCellComment();
    }

    private static void copyCellValue(Cell cellFrom, Cell cellTo) {
        cellTo.setBlank();
        switch (cellFrom.getCellType()) {
            case BLANK:
                break;
            case BOOLEAN:
                cellTo.setCellValue(cellFrom.getBooleanCellValue());
                break;
            case FORMULA:
                cellTo.setCellFormula(cellFrom.getCellFormula());
                try {
                    PoiExcelHelper.evaluateFormula(cellTo);
                } catch (Exception ignored) {
                }
                break;
            case NUMERIC:
                cellTo.setCellValue(cellFrom.getNumericCellValue());
                break;
            case STRING:
                cellTo.setCellValue(cellFrom.getRichStringCellValue());
                break;
            default:
                throw new RuntimeException("Unknown cell type: " + cellFrom.getCellType());
        }
    }

    @Override
    public void createCell(int col, int row, Object value, String formula, ICellStyle style, String comment, String prevCommentAuthor) {
        if (StringUtils.isNotBlank(formula)) {
            setCellFormula(col, row, formula);
        } else {
            setCellValue(col, row, value);
        }
        setCellStyle(col, row, style);
        setCellComment(col, row, comment, prevCommentAuthor);
    }

    @Override
    public IGridRegion findEmptyRect(int width, int height) {
        int lastRow = getSheet().getLastRowNum();
        int top = lastRow + 2;
        int left = 1;

        GridRegion newRegion = new GridRegion(top, left, top + height - 1, left + width - 1);
        if (IGridRegion.Tool.isValidRegion(newRegion, getSpreadsheetConstants())) {
            return newRegion;
        }
        return null;
    }

    @Override
    public ICell getCell(int column, int row) {
        return new XlsCell(column, row, this);
    }

    @Override
    public int getColumnWidth(int col) {
        Sheet sheet = getSheet();
        int w = sheet.getColumnWidth((short) col);
        if (w == sheet.getDefaultColumnWidth()) {
            return 79;
        }
        return w / 40;
    }

    @Override
    public int getMaxColumnIndex(int rownum) {
        Row row = getSheet().getRow(rownum);
        return row == null ? 0 : row.getLastCellNum();
    }

    @Override
    public int getMaxRowIndex() {
        return getSheet().getLastRowNum();
    }

    @Override
    public synchronized IGridRegion getMergedRegion(int i) {
        return new XlsGridRegion(getSheet().getMergedRegion(i));
    }

    @Override
    public int getMinColumnIndex(int rownum) {
        Row row = getSheet().getRow(rownum);
        return row == null ? 0 : row.getFirstCellNum();
    }

    @Override
    public int getMinRowIndex() {
        return getSheet().getFirstRowNum();
    }

    public String getName() {
        return sheetSource.getSheetName();
    }

    @Override
    public int getNumberOfMergedRegions() {
        try {
            return getSheet().getNumMergedRegions();
        } catch (NullPointerException e) {
            return 0;
        }
    }

    /**
     * Gets the URI to the table by table region. Just calls {@link XlsSheetGridModel#getRangeUri(int, int, int, int)}.
     *
     * @param region Table region.
     * @return URI to the table in the sheet. (e.g. <code>file:D:\work\Workspace\org.openl.tablets.tutorial4\rules
     * \main&wbName=Tutorial_4.xls&wsName=Vehicle-Scoring&range=B3:D12</code>)
     */
    public String getRangeUri(IGridRegion region) {
        return getRangeUri(region.getLeft(), region.getTop(), region.getRight(), region.getBottom());
    }

    @Override
    public IGridRegion getRegionContaining(int x, int y) {
        return getMergedRegionsPool().getRegionContaining(x, y);
    }

    public XlsSheetSourceCodeModule getSheetSource() {
        return sheetSource;
    }

    @Override
    public String getUri() {
        return sheetSource == null ? "" : sheetSource.getUri();// + "#" + name;
    }

    @Override
    public boolean isEmpty(int x, int y) {
        Cell cell = PoiExcelHelper.getCell(x, y, getSheet());
        if (cell == null) {
            return true;
        }

        final CellType cellType = cell.getCellType();
        if (cellType == CellType.BLANK) {
            return true;
        }

        if (cellType == CellType.STRING) {
            String v = cell.getStringCellValue();
            return StringUtils.isBlank(v);
        }
        return false;
    }

    @Override
    public void removeMergedRegion(IGridRegion remove) {
        removeMergedRegion(remove.getLeft(), remove.getTop());
    }

    @Override
    public void removeMergedRegion(int x, int y) {
        Sheet sheet = getSheet();
        getMergedRegionsPool().remove(x, y);
        int nregions = getNumberOfMergedRegions();
        for (int i = 0; i < nregions; i++) {
            CellRangeAddress reg = sheet.getMergedRegion(i);
            if (reg.getFirstColumn() == x && reg.getFirstRow() == y) {
                sheet.removeMergedRegion(i);
                return;
            }
        }
    }

    @Override
    public void setCellValue(int col, int row, Object value) {
        Cell poiCell = PoiExcelHelper.getOrCreateCell(col, row, getSheet());
        if (value != null) {
            AXlsCellWriter cellWriter = getCellWriter(value);
            cellWriter.setCellToWrite(poiCell);
            cellWriter.setValueToWrite(value);
            cellWriter.writeCellValue();
        } else {
            poiCell.setBlank();
        }
    }

    @Override
    public void setCellStringValue(int col, int row, String value) {
        Cell cell = PoiExcelHelper.getOrCreateCell(col, row, getSheet());
        cell.setCellValue(value);
    }

    @Override
    public void setCellFormula(int col, int row, String formula) {
        Cell poiCell = PoiExcelHelper.getOrCreateCell(col, row, getSheet());

        if (formula != null) {
            AXlsCellWriter cellWriter = getCellWriters().get(AXlsCellWriter.FORMULA_WRITER);
            cellWriter.setCellToWrite(poiCell);
            cellWriter.setValueToWrite(formula);
            cellWriter.writeCellValue();
        }
    }

    private Map<String, AXlsCellWriter> getCellWriters() {
        if (cellWriters.isEmpty()) {
            initCellWriters();
        }
        return cellWriters;
    }

    @Override
    public void setCellStyle(int col, int row, ICellStyle style) {
        Sheet sheet = getSheet();
        Cell poiCell = PoiExcelHelper.getOrCreateCell(col, row, sheet);
        CellStyle newPoiStyle;
        CellStyle styleToClone;

        if (style instanceof XlsCellStyle) {
            newPoiStyle = ((XlsCellStyle) style).getXlsStyle();
            newPoiStyle.cloneStyleFrom(newPoiStyle);
        } /*
         * else if (style instanceof org.openl.rules.table.ui.CellStyle) { styleToClone = poiCell.getCellStyle();
         * newPoiStyle.cloneStyleFrom(styleToClone);
         *
         * setCellStyle(newPoiStyle, style); }
         */ else {
            newPoiStyle = PoiExcelHelper.createCellStyle(sheet.getWorkbook());
            styleToClone = poiCell.getCellStyle();
            newPoiStyle.cloneStyleFrom(styleToClone);
        }

        poiCell.setCellStyle(newPoiStyle);
    }

    /*
     * Set only BorderStyle and BorderRGB properties
     */
    @Override
    public void setCellBorderStyle(int col, int row, ICellStyle style) {
        if (style == null) {
            // no needs to set absent styles.
            return;
        }
        Sheet sheet = getSheet();
        Cell poiCell = PoiExcelHelper.getOrCreateCell(col, row, sheet);
        CellStyle newPoiStyle = PoiExcelHelper.createCellStyle(sheet.getWorkbook());

        newPoiStyle.cloneStyleFrom(poiCell.getCellStyle());

        if (style.getBorderStyle() != null) {
            BorderStyle[] borderStyle = style.getBorderStyle();

            newPoiStyle.setBorderTop(borderStyle[0]);
            newPoiStyle.setBorderRight(borderStyle[1]);
            newPoiStyle.setBorderBottom(borderStyle[2]);
            newPoiStyle.setBorderLeft(borderStyle[3]);

        }

        if (style.getBorderRGB() != null) {
            PoiExcelHelper.setCellBorderColors(newPoiStyle, style.getBorderRGB(), sheet.getWorkbook());
        }

        poiCell.setCellStyle(newPoiStyle);
    }

    @Override
    public void setCellAlignment(int col, int row, HorizontalAlignment alignment) {
        Sheet sheet = getSheet();
        Cell cell = PoiExcelHelper.getOrCreateCell(col, row, sheet);
        CellUtil.setCellStyleProperty(cell, CellUtil.ALIGNMENT, alignment);
    }

    @Override
    public void setCellIndent(int col, int row, int indent) {
        Sheet sheet = getSheet();
        Cell cell = PoiExcelHelper.getOrCreateCell(col, row, sheet);
        CellUtil.setCellStyleProperty(cell, CellUtil.INDENTION, (short) indent);
    }

    @Override
    public void setCellFillColor(int col, int row, short[] color) {
        Cell cell = PoiExcelHelper.getOrCreateCell(col, row, getSheet());
        CellStyle newStyle = PoiExcelHelper.cloneStyleFrom(cell);

        if (color != null) {
            if (newStyle.getFillPattern() == FillPatternType.NO_FILL) {
                newStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            }
            setCellFillColor(newStyle, color);
        } else {
            newStyle.setFillPattern(FillPatternType.NO_FILL);
        }

        cell.setCellStyle(newStyle);
    }

    private void setCellFillColor(CellStyle dest, short[] rgb) {
        // Xlsx
        if (dest instanceof XSSFCellStyle) {
            XSSFWorkbook workbook = (XSSFWorkbook) getSheet().getWorkbook();
            IndexedColorMap indexedColors = workbook.getStylesSource().getIndexedColors();
            XSSFColor color = new XSSFColor(convertRGB(rgb), indexedColors);
            ((XSSFCellStyle) dest).setFillForegroundColor(color);

            // Xls
        } else {
            Short color = findIndexedColor(rgb);
            if (color != null) {
                dest.setFillForegroundColor(color);
            }
        }
    }

    @Override
    public void setCellFontColor(int col, int row, short[] color) {
        Sheet sheet = getSheet();
        Cell cell = PoiExcelHelper.getOrCreateCell(col, row, sheet);
        Workbook workbook = sheet.getWorkbook();

        CellStyle newStyle = PoiExcelHelper.cloneStyleFrom(cell);
        Font newFont = workbook.createFont();
        int fontIndex = cell.getCellStyle().getFontIndexAsInt();
        Font fromFont = workbook.getFontAt(fontIndex);

        newFont.setBold(fromFont.getBold());
        newFont.setColor(fromFont.getColor());
        newFont.setFontHeight(fromFont.getFontHeight());
        newFont.setFontName(fromFont.getFontName());
        newFont.setItalic(fromFont.getItalic());
        newFont.setStrikeout(fromFont.getStrikeout());
        newFont.setTypeOffset(fromFont.getTypeOffset());
        newFont.setUnderline(fromFont.getUnderline());
        newFont.setCharSet(fromFont.getCharSet());

        if (color != null) {
            // Xlsx
            if (newFont instanceof XSSFFont) {
                IndexedColorMap indexedColors = ((XSSFWorkbook) workbook).getStylesSource().getIndexedColors();
                XSSFColor color1 = new XSSFColor(convertRGB(color), indexedColors);
                ((XSSFFont) newFont).setColor(color1);

                // Xls
            } else {
                Short color1 = findIndexedColor(color);
                if (color1 != null) {
                    newFont.setColor(color1);
                }
            }
        } else {
            newFont.setColor(HSSFColor.HSSFColorPredefined.BLACK.getIndex());
        }

        newStyle.setFont(newFont);
        cell.setCellStyle(newStyle);
    }

    private Short findIndexedColor(short[] rgb) {
        HSSFPalette palette = ((HSSFWorkbook) getSheet().getWorkbook()).getCustomPalette();
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
        return color == null ? null : color.getIndex();
    }

    @Override
    public void setCellFontBold(int col, int row, boolean bold) {
        Cell cell = PoiExcelHelper.getOrCreateCell(col, row, getSheet());
        PoiExcelHelper.setCellFontBold(cell, bold);
    }

    @Override
    public void setCellFontItalic(int col, int row, boolean italic) {
        Cell cell = PoiExcelHelper.getOrCreateCell(col, row, getSheet());
        PoiExcelHelper.setCellFontItalic(cell, italic);
    }

    @Override
    public void setCellFontUnderline(int col, int row, boolean underlined) {
        Cell cell = PoiExcelHelper.getOrCreateCell(col, row, getSheet());
        byte underline = underlined ? Font.U_SINGLE : Font.U_NONE;
        PoiExcelHelper.setCellFontUnderline(cell, underline);
    }

    @Override
    public void setCellComment(int col, int row, String comment, String prevCommentAuthor) {
        Cell poiCell = PoiExcelHelper.getOrCreateCell(col, row, getSheet());
        Comment poiComment = null;
        if (comment != null) {
            Sheet sheet = getSheet();
            CreationHelper factory = sheet.getWorkbook().getCreationHelper();
            ClientAnchor anchor = factory.createClientAnchor();
            anchor.setCol1(poiCell.getColumnIndex());
            anchor.setCol2(poiCell.getColumnIndex());
            anchor.setRow1(poiCell.getRowIndex());
            anchor.setRow2(poiCell.getRowIndex());
            if (poiCell.getCellComment() == null) {
                poiComment = sheet.createDrawingPatriarch().createCellComment(anchor);
            } else {
                poiComment = poiCell.getCellComment();
            }
            poiComment.setString(factory.createRichTextString(comment));
            poiComment.setAuthor(prevCommentAuthor);
        }
        poiCell.setCellComment(poiComment);
    }

    // TODO: move to factory.
    public AXlsCellWriter getCellWriter(Object value) {
        Map<String, AXlsCellWriter> cellWriters = getCellWriters();
        AXlsCellWriter result;
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
            if (strValue.startsWith("=")) {
                result = cellWriters.get(AXlsCellWriter.FORMULA_WRITER);
            } else {
                result = cellWriters.get(AXlsCellWriter.STRING_WRITER);
            }
        }
        return result;
    }

    private Sheet getSheet() {
        return sheetSource.getSheet();
    }

    private byte[] convertRGB(short[] rgb) {
        return new byte[]{(byte) rgb[0], (byte) rgb[1], (byte) rgb[2]};
    }

    public SpreadsheetConstants getSpreadsheetConstants() {
        return sheetSource.getWorkbookSource().getWorkbookLoader().getSpreadsheetConstants();
    }

}
