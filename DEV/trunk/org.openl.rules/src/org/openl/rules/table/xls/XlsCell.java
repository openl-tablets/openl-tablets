package org.openl.rules.table.xls;

import java.util.Date;

import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openl.rules.lang.xls.types.CellMetaInfo;
import org.openl.rules.table.GridRegion;
import org.openl.rules.table.ICell;
import org.openl.rules.table.IGrid;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.ui.ICellFont;
import org.openl.rules.table.ui.ICellStyle;
import org.openl.util.NumberUtils;

public class XlsCell implements ICell {

    private int column;
    private int row;
    private IGridRegion region;
    private Cell cell;

    private int width = 1;
    private int height = 1;

    private XlsSheetGridModel gridModel;
    
    /**
     * Usually there is a parameter duplication: the same column and row exist in cell object.
     * But sometimes cell is null, so we will have just the coordinates of the cell.
     */
    private XlsCell(int column, int row, IGridRegion region, Cell cell) {
        this.column = column;
        this.row = row;
        this.region = region;
        this.cell = cell;
        
        if (region != null && region.getLeft() == column && region.getTop() == row) {
            this.width = region.getRight() - region.getLeft() + 1;
            this.height = region.getBottom() - region.getTop() + 1;
        }
    }

    public XlsCell(int column, int row, XlsSheetGridModel gridModel) {
        this(column, row, gridModel.getRegionContaining(column, row), PoiHelper.getPoiXlsCell(column, row, gridModel.getSheetSource().getSheet()));
        this.gridModel = gridModel;
    }

    public ICellStyle getStyle() {
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
        if (cell == null && region == null) {
            return null;
        } else if (region != null) { // If cell belongs to some merged region, we try to get merged value from it.                
            return extractValueFromRegion();
        } else {
            return extractCellValue(true);
        }
    }
    
    private ICell getTopLeftCellFromRegion() {
        // Gets the top left cell in this region
        int row = region.getTop();
        int col = region.getLeft();
        return gridModel.getCell(col, row);
    }

    private boolean isCurrentCellATopLeftCellInRegion() {
        ICell topLeftCell = getTopLeftCellFromRegion();
        if (topLeftCell.getColumn() == this.column && topLeftCell.getRow() == this.row) {
            return true;
        }
        return false;
    }

    private Object extractValueFromRegion() {   
        // If the top left cell is the current cell instance, we just extract it`s value.
        // In other case get string value of top left cell of the region.
        if (isCurrentCellATopLeftCellInRegion()) {
            return extractCellValue(true);
        } else {
            ICell topLeftCell = getTopLeftCellFromRegion();
            return topLeftCell.getObjectValue();
        }            
    }

    private Object extractCellValue(boolean useCachedValue){
        if (cell != null) {
            int type = cell.getCellType();
            if (useCachedValue && type == Cell.CELL_TYPE_FORMULA)
                type = cell.getCachedFormulaResultType();
            switch (type) {
                case Cell.CELL_TYPE_BLANK:
                    return null;
                case Cell.CELL_TYPE_BOOLEAN:
                    return Boolean.valueOf(cell.getBooleanCellValue());
                case Cell.CELL_TYPE_NUMERIC:
                    if (DateUtil.isCellDateFormatted(cell)) {
                        return cell.getDateCellValue();
                    }
                    double value = cell.getNumericCellValue();
                    return NumberUtils.intOrDouble(value);
                case Cell.CELL_TYPE_STRING:
                    return cell.getStringCellValue();
                case Cell.CELL_TYPE_FORMULA:
                    try {
                        FormulaEvaluator formulaEvaluator = gridModel.getSheetSource().getSheet().getWorkbook().getCreationHelper().createFormulaEvaluator();
                        formulaEvaluator.evaluateFormulaCell(cell);
                    } catch (RuntimeException e) {
                    }
                    //extract new calculated value or previously cached value if calculation failed
                    return extractCellValue(true);
                default:
                    return "unknown type: " + cell.getCellType();
            }
        }
        return null;
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
        if (cell == null && region == null) {
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
        return cell.getCellType() == IGrid.CELL_TYPE_FORMULA ? cell.getCellFormula() : null;
    }

    public int getType() {
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
            return cell.getCellType();
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
        if (cell == null) {
            return 0;
        }
        return cell.getNumericCellValue();
    }
        
    public int getNativeType() {
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
        if (cell == null) {
            return null;
        }
        return cell.getDateCellValue();
    }

    public CellMetaInfo getMetaInfo() {
        return gridModel.getCellMetaInfo(column, row);
    }
    
    private ICellStyle getCellStyle(Cell cell) {
        CellStyle style = cell.getCellStyle();
        if (style != null) {
            Workbook workbook = gridModel.getSheetSource().getSheet().getWorkbook();
            if (style instanceof XSSFCellStyle) {
                return new XlsCellStyle2((XSSFCellStyle) style, (XSSFWorkbook) workbook);
            } else {
                return new XlsCellStyle((HSSFCellStyle) style, (HSSFWorkbook) workbook);
            }
        }
        return null;
    }

}