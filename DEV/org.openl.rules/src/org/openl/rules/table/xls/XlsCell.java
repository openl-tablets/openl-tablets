package org.openl.rules.table.xls;

import java.util.Date;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Comment;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Workbook;
import org.openl.rules.lang.xls.load.CellLoader;
import org.openl.rules.lang.xls.types.CellMetaInfo;
import org.openl.rules.table.GridRegion;
import org.openl.rules.table.ICell;
import org.openl.rules.table.ICellComment;
import org.openl.rules.table.IGrid;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.ui.ICellFont;
import org.openl.rules.table.ui.ICellStyle;
import org.openl.rules.table.xls.formatters.XlsDataFormatterFactory;
import org.openl.rules.table.xls.writers.AXlsCellWriter;
import org.openl.util.NumberUtils;
import org.openl.util.StringPool;
import org.openl.util.StringUtils;
import org.openl.util.formatters.IFormatter;

public class XlsCell implements ICell {

    private int column;
    private int row;
    private IGridRegion region;
    private CellLoader cellLoader;

    private int width = 1;
    private int height = 1;

    private XlsSheetGridModel gridModel;

    /**
     * Usually there is a parameter duplication: the same column and row exist in cell object.
     * But sometimes cell is null, so we will have just the coordinates of the cell.
     */
    public XlsCell(int column, int row, XlsSheetGridModel gridModel) {
        this.column = column;
        this.row = row;
        this.region = gridModel.getRegionContaining(column, row);
        this.cellLoader = gridModel.getSheetSource().getSheetLoader().getCellLoader(column, row);

        if (region != null && region.getLeft() == column && region.getTop() == row) {
            this.width = region.getRight() - region.getLeft() + 1;
            this.height = region.getBottom() - region.getTop() + 1;
        }
        this.gridModel = gridModel;
    }

    public ICellStyle getStyle() {
        Cell cell = getCell();
        if (cell == null) return null;
        return getCellStyle(cell);
    }

    public int getAbsoluteColumn() {
        return getColumn();
    }

    public int getAbsoluteRow() {
        return getRow();
    }

    public IGridRegion getAbsoluteRegion() {
        IGridRegion absoluteRegion = getRegion();
        if (absoluteRegion == null) {
            absoluteRegion = new GridRegion(row, column, row, column);
        }
        return absoluteRegion;
    }

    public int getColumn() {
        return column;
    }

    public ICellFont getFont() {
        Cell cell = getCell();
        if (cell == null) return null;
        Font font = gridModel.getSheetSource().getSheet().getWorkbook().getFontAt(cell.getCellStyle().getFontIndex());
        return new XlsCellFont(font, gridModel.getSheetSource().getSheet().getWorkbook());
    }

    public int getRow() {
        return row;
    }

    public IGridRegion getRegion() {
        return region;
    }

    public int getHeight() {
        return height;
    }

    public int getWidth() {
        return width;
    }

    public Object getObjectValue() {
        if (region == null || isCurrentCellATopLeftCellInRegion()) {
            // If cell belongs to some merged region, we try to get merged value from it.
            // If the top left cell is the current cell instance, we just extract it`s value.
            // In other case get string value of top left cell of the region.
            return extractCellValue();
        } else {
            ICell topLeftCell = getTopLeftCellFromRegion();
            return topLeftCell.getObjectValue();
        }
    }

    @SuppressWarnings("deprecation")
    public void setObjectValue(Object value) {
        Cell cell = getCell();
        if (value != null) {
            boolean writeCellMetaInfo = true;

            // Don't write meta info for predefined String arrays to avoid
            // removing Enum Domain meta info.
            if (gridModel.hasEnumDomainMetaInfo(column, row)) {
                writeCellMetaInfo = false;
            }

            // Don't write meta info for predefined String arrays to avoid
            // removing Range Domain meta info.
            if (gridModel.hasRangeDomainMetaInfo(column, row)) {
                writeCellMetaInfo = false;
            }

            AXlsCellWriter cellWriter = gridModel.getCellWriter(value);
            cellWriter.setCellToWrite(cell);
            cellWriter.setValueToWrite(value);
            cellWriter.writeCellValue(writeCellMetaInfo);
        } else {
            cell.setCellType(IGrid.CELL_TYPE_BLANK);
        }
    }

    public String getStringValue() {
        Object res = getObjectValue();
        return res == null ? null : String.valueOf(res);
    }

    public void setStringValue(String value) {
        getCell().setCellValue(value);
    }

    public String getFormattedValue() {
        String formattedValue = null;

        Object value = getObjectValue();

        if (value != null) {
            IFormatter cellDataFormatter = getDataFormatter();

            if (cellDataFormatter == null && value instanceof Date) {
                // Cell type is unknown but in Excel it's stored as a Date.
                // We can't override getDataFormatter() or XlsDataFormatterFactory.getFormatter() to support this case
                // because they are also invoked when editing a cell. When editing cells with unknown type null must be
                // returned to be able to edit such cell as if it can contain any text.
                // But we can safely format it's value when just viewing it's value.
                cellDataFormatter = XlsDataFormatterFactory.getDateFormatter(this);
            }

            if (cellDataFormatter != null) {
                formattedValue = cellDataFormatter.format(value);
            }
        }

        if (formattedValue == null) {
            formattedValue = getStringValue();
            if (formattedValue == null) {
                formattedValue = StringUtils.EMPTY;
            }
        }

        return formattedValue;
    }

    public IFormatter getDataFormatter() {
        return XlsDataFormatterFactory.getFormatter(this);
    }

    @Override
    public ICell getTopLeftCellFromRegion() {
        // Gets the top left cell in this region
        int row = region.getTop();
        int col = region.getLeft();
        return gridModel.getCell(col, row);
    }

    private boolean isCurrentCellATopLeftCellInRegion() {
        ICell topLeftCell = getTopLeftCellFromRegion();
        return topLeftCell.getColumn() == this.column && topLeftCell.getRow() == this.row;
    }

    private Object extractCellValue() {
        Cell cell = getCell();
        if (cell != null) {
            int type = cell.getCellType();
            if (type == Cell.CELL_TYPE_FORMULA) {
                // Replace Cell.CELL_TYPE_FORMULA with the type from the formula result
                type = cell.getCachedFormulaResultType();
            }
            // There Cell.CELL_TYPE_FORMULA should never be at this step
            switch (type) {
                case Cell.CELL_TYPE_BLANK:
                    return null;
                case Cell.CELL_TYPE_BOOLEAN:
                    return cell.getBooleanCellValue();
                case Cell.CELL_TYPE_NUMERIC:
                    if (DateUtil.isCellDateFormatted(cell)) {
                        return cell.getDateCellValue();
                    }
                    double value = cell.getNumericCellValue();
                    return NumberUtils.intOrDouble(value);
                case Cell.CELL_TYPE_STRING:
                    String str = StringUtils.trimToNull(cell.getStringCellValue());
                    return StringPool.intern(str);
                default:
                    return "unknown type: " + cell.getCellType();
            }
        }
        return null;
    }

    public String getFormula() {
        if (getCell() == null && region == null) {
            return null;
        } else if (region != null) {
            return getFormulaFromRegion();
        } else {
            return cellFormula();
        }
    }

    private String getFormulaFromRegion() {
        if (isCurrentCellATopLeftCellInRegion()) {
            return cellFormula();
        }
        ICell topLeftCell = getTopLeftCellFromRegion();
        return topLeftCell.getType() == IGrid.CELL_TYPE_FORMULA ? topLeftCell.getFormula() : null;
    }

    private String cellFormula() {
        Cell cell = getCell();
        return cell.getCellType() == IGrid.CELL_TYPE_FORMULA ? cell.getCellFormula() : null;
    }

    public int getType() {
        Cell cell = getCell();
        if (cell == null && region == null) {
            return Cell.CELL_TYPE_BLANK;
        } else if (region != null) {
            return getTypeFromRegion();
        } else {
            return cell.getCellType();
        }

    }

    private int getTypeFromRegion() {
        if (isCurrentCellATopLeftCellInRegion()) {
            return getCell().getCellType();
        }
        ICell topLeftCell = getTopLeftCellFromRegion();
        return topLeftCell.getType();
    }

    public String getUri() {
        return XlsUtil.xlsCellPresentation(column, row);
    }

    public boolean getNativeBoolean() {
        return true;
    }

    public double getNativeNumber() {
        Cell cell = getCell();
        if (cell == null) {
            return 0;
        }
        return cell.getNumericCellValue();
    }

    public int getNativeType() {
        Cell cell = getCell();
        if (cell == null) {
            return IGrid.CELL_TYPE_BLANK;
        }

        int type = cell.getCellType();
        if (type == IGrid.CELL_TYPE_FORMULA) {
            return cell.getCachedFormulaResultType();
        }
        return type;
    }

    public boolean hasNativeType() {
        return true;
    }

    /**
     * @return date value if cell is of type {@link IGrid#CELL_TYPE_NUMERIC} and is formatted in excel as date.<br>
     * null is cell is of type {@link IGrid#CELL_TYPE_NUMERIC} and is not formatted in excel as date.<br>
     * @throws IllegalStateException is the cell is of type {@link IGrid#CELL_TYPE_STRING}
     */
    public Date getNativeDate() {
        Cell cell = getCell();
        if (cell == null) {
            return null;
        }
        try {
            return cell.getDateCellValue();
        } catch (NullPointerException npe){
            throw new IllegalStateException("Cannot parse the value as a date : " + cell.getNumericCellValue());
        }
    }

    public CellMetaInfo getMetaInfo() {
        return gridModel.getCellMetaInfo(column, row);
    }

    public void setMetaInfo(CellMetaInfo metaInfo) {
        gridModel.setCellMetaInfo(column, row, metaInfo);
    }

    private ICellStyle getCellStyle(Cell cell) {
        CellStyle style = cell.getCellStyle();
        if (style != null) {
            Workbook workbook = gridModel.getSheetSource().getSheet().getWorkbook();
            return new XlsCellStyle(style, workbook);
        }
        return null;
    }

    public ICellComment getComment() {
        Cell cell = getCell();
        if (cell != null) {
            Comment comment = cell.getCellComment();
            if (comment != null) {
                return new XlsCellComment(comment);
            } else if (region != null && !isCurrentCellATopLeftCellInRegion()) {
                ICell topLeftCell = getTopLeftCellFromRegion();
                return topLeftCell.getComment();
            }
        }
        return null;
    }

    public Cell getXlsCell() {
        return getCell();
    }

    private Cell getCell() {
        return cellLoader.getCell();
    }
}