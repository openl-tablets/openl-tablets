package org.openl.rules.xls.merge;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Comment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFPicture;
import org.apache.poi.xssf.usermodel.XSSFShape;

/**
 * Internal cursor, that points to current iteration state
 */
class Cursor {

    /**
     * Excel workbook
     */
    public final Workbook workbook;
    /**
     * Sheet of current {@code workbook}
     */
    public final Sheet sheet;
    /**
     * Row of current {@code sheet}
     */
    public Row row;
    /**
     * Cell of current {@code row}
     */
    public Cell cell;
    /**
     * CellStyle of current {@code cell}
     */
    public CellStyle cellStyle;
    /**
     * Comment of current {@code cell}
     */
    public Comment comment;
    /**
     * Picture of current {@code sheet}
     */
    public XSSFPicture picture;

    public final Collection<Cell> formulas;

    public Cursor(Workbook workbook, Sheet sheet) {
        this.workbook = workbook;
        this.sheet = sheet;
        formulas = null;
    }

    public Cursor(Workbook workbook, Sheet sheet, Collection<Cell> formulas) {
        this.workbook = workbook;
        this.sheet = sheet;
        this.formulas = formulas;
    }

    /**
     * Get merged region of current {@code cell}
     *
     * @return merged region of {@code null}
     */
    public CellRangeAddress getCellMergedRegion() {
        for (CellRangeAddress range : sheet.getMergedRegions()) {
            if (range.isInRange(cell)) {
                return range;
            }
        }
        return null;
    }

    /**
     * Get hidden flag of current {@code sheet}
     *
     * @return {@code true} or {@code false}
     */
    public boolean isSheetHidden() {
        return workbook.isSheetHidden(getSheetIndex());
    }

    /**
     * Get index of current {@code sheet}
     *
     * @return index of the sheet (0 based)
     */
    public int getSheetIndex() {
        return workbook.getSheetIndex(sheet.getSheetName());
    }

    /**
     * Get shapes of current {@code sheet}
     *
     * @return shapes collection or empty
     */
    public List<XSSFShape> getSheetShapes() {
        return Optional.ofNullable(sheet.getDrawingPatriarch())
                .filter(XSSFDrawing.class::isInstance)
                .map(XSSFDrawing.class::cast)
                .map(XSSFDrawing::getShapes)
                .orElse(Collections.emptyList());
    }

    /**
     * Get pictures of current {@code sheet}
     *
     * @return pictures collection or empty
     */
    public List<XSSFPicture> getSheetPictures() {
        return getSheetShapes().stream()
                .filter(XSSFPicture.class::isInstance)
                .map(XSSFPicture.class::cast)
                .collect(Collectors.toList());
    }

    /**
     * Get physical numbers of cells of current {@code sheet}
     *
     * @return int representing the number of defined cells in the row.
     */
    public int getSheetPhysicalNumberOfCells() {
        var rowIter = sheet.rowIterator();
        return rowIter.hasNext() ? rowIter.next().getPhysicalNumberOfCells() : 0;
    }

    public void addFormulaCell(Cell formulaCell) {
        if (formulas != null) {
            formulas.add(formulaCell);
        }
    }

    public Workbook originalWorkbook() {
        if (workbook instanceof StreamWorkbook) {
            return ((StreamWorkbook) workbook).unwrap();
        }
        return workbook;
    }
}
