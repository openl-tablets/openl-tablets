package org.openl.rules.table.xls;

import java.util.Date;

import org.apache.poi.ss.usermodel.*;
import org.openl.rules.lang.xls.load.CellLoader;
import org.openl.rules.table.*;
import org.openl.rules.table.ui.ICellFont;
import org.openl.rules.table.ui.ICellStyle;
import org.openl.util.NumberUtils;
import org.openl.util.StringPool;
import org.openl.util.StringUtils;

public class XlsCell implements ICell {

    private int column;
    private int row;
    private IGridRegion region;
    private CellLoader cellLoader;

    private int width = 1;
    private int height = 1;

    private XlsSheetGridModel gridModel;

    /**
     * Usually there is a parameter duplication: the same column and row exist in cell object. But sometimes cell is
     * null, so we will have just the coordinates of the cell.
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

    @Override
    public ICellStyle getStyle() {
        Cell cell = getCell();
        if (cell == null)
            return null;
        return getCellStyle(cell);
    }

    @Override
    public int getAbsoluteColumn() {
        return getColumn();
    }

    @Override
    public int getAbsoluteRow() {
        return getRow();
    }

    @Override
    public IGridRegion getAbsoluteRegion() {
        IGridRegion absoluteRegion = getRegion();
        if (absoluteRegion == null) {
            absoluteRegion = new GridRegion(row, column, row, column);
        }
        return absoluteRegion;
    }

    @Override
    public int getColumn() {
        return column;
    }

    @Override
    public ICellFont getFont() {
        Cell cell = getCell();
        if (cell == null)
            return null;
        Font font = gridModel.getSheetSource()
            .getSheet()
            .getWorkbook()
            .getFontAt(cell.getCellStyle().getFontIndexAsInt());
        return new XlsCellFont(font, gridModel.getSheetSource().getSheet().getWorkbook());
    }

    @Override
    public int getRow() {
        return row;
    }

    @Override
    public IGridRegion getRegion() {
        return region;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
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

    @Override
    public String getStringValue() {
        Object res = getObjectValue();
        return res == null ? null : String.valueOf(res);
    }

    public void setStringValue(String value) {
        getCell().setCellValue(value);
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
                    String str = StringUtils.trimToNull(cell.getStringCellValue());
                    return StringPool.intern(str);
                default:
                    return "unknown type: " + cell.getCellType();
            }
        }
        return null;
    }

    @Override
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
        return cell.getCellType() == CellType.FORMULA ? cell.getCellFormula() : null;
    }

    @Override
    public int getType() {
        Cell cell = getCell();
        if (cell == null && region == null) {
            return IGrid.CELL_TYPE_BLANK;
        } else if (region != null) {
            return getTypeFromRegion();
        } else {
            return getIGridCellType(cell.getCellType());
        }

    }

    private int getTypeFromRegion() {
        if (isCurrentCellATopLeftCellInRegion()) {
            return getIGridCellType(getCell().getCellType());
        }
        ICell topLeftCell = getTopLeftCellFromRegion();
        return topLeftCell.getType();
    }

    @Override
    public String getUri() {
        return XlsUtil.xlsCellPresentation(column, row);
    }

    @Override
    public boolean getNativeBoolean() {
        return true;
    }

    @Override
    public double getNativeNumber() {
        Cell cell = getCell();
        if (cell == null) {
            return 0;
        }
        return cell.getNumericCellValue();
    }

    @Override
    public int getNativeType() {
        Cell cell = getCell();
        if (cell == null) {
            return IGrid.CELL_TYPE_BLANK;
        }

        CellType type = cell.getCellType();
        if (type == CellType.FORMULA) {
            return getIGridCellType(cell.getCachedFormulaResultType());
        }
        return getIGridCellType(type);
    }

    @Override
    public boolean hasNativeType() {
        return true;
    }

    /**
     * @return date value if cell is of type {@link IGrid#CELL_TYPE_NUMERIC} and is formatted in excel as date.<br>
     *         null is cell is of type {@link IGrid#CELL_TYPE_NUMERIC} and is not formatted in excel as date.<br>
     * @throws IllegalStateException is the cell is of type {@link IGrid#CELL_TYPE_STRING}
     */
    @Override
    public Date getNativeDate() {
        Cell cell = getCell();
        if (cell == null) {
            return null;
        }
        try {
            return cell.getDateCellValue();
        } catch (NullPointerException npe) {
            throw new IllegalStateException("Cannot parse the value as a date : " + cell.getNumericCellValue());
        }
    }

    private ICellStyle getCellStyle(Cell cell) {
        CellStyle style = cell.getCellStyle();
        if (style != null) {
            Workbook workbook = gridModel.getSheetSource().getSheet().getWorkbook();
            return new XlsCellStyle(style, workbook);
        }
        return null;
    }

    @Override
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

    private Cell getCell() {
        return cellLoader.getCell();
    }

    private int getIGridCellType(CellType cellType) {
        switch (cellType) {
            case NUMERIC:
                return IGrid.CELL_TYPE_NUMERIC;
            case STRING:
                return IGrid.CELL_TYPE_STRING;
            case FORMULA:
                return IGrid.CELL_TYPE_FORMULA;
            case BLANK:
                return IGrid.CELL_TYPE_BLANK;
            case BOOLEAN:
                return IGrid.CELL_TYPE_BOOLEAN;
            case ERROR:
                return IGrid.CELL_TYPE_ERROR;
            default:
                throw new IllegalArgumentException("Unsupported cell type " + cellType);
        }
    }
}