package org.openl.rules.xls.merge;

import static org.openl.rules.xls.merge.XlsSheetsMatcher.getCommentAuthor;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.Shape;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellUtil;
import org.apache.poi.ss.util.SheetUtil;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFPicture;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.xmlbeans.XmlObject;
import org.openxmlformats.schemas.drawingml.x2006.main.CTBlip;
import org.openxmlformats.schemas.drawingml.x2006.main.CTBlipFillProperties;
import org.openxmlformats.schemas.drawingml.x2006.spreadsheetDrawing.CTAbsoluteAnchor;
import org.openxmlformats.schemas.drawingml.x2006.spreadsheetDrawing.CTOneCellAnchor;
import org.openxmlformats.schemas.drawingml.x2006.spreadsheetDrawing.CTPicture;
import org.openxmlformats.schemas.drawingml.x2006.spreadsheetDrawing.CTTwoCellAnchor;

/**
 * This service copies all content from one sheet to another
 *
 * @author Vladyslab Pikus
 */
public final class XlsSheetCopier {

    private XlsSheetCopier() {
    }

    /**
     * Copy content from {@code srcSheet} to {@code destSheet}. If {@code destSheet} has content, it will be erased
     *
     * @param srcWorkbook  source workbook
     * @param srcSheet     source sheet
     * @param destWorkbook destination workbook
     * @param destSheet    destination sheet
     * @param formulas     formulas to evaluate
     * @throws IOException if happened while copy
     */
    public static void copy(Workbook srcWorkbook,
                            Sheet srcSheet,
                            Workbook destWorkbook,
                            Sheet destSheet,
                            Collection<Cell> formulas) throws IOException {
        // prepare sheet
        removeAllRows(destSheet);
        // do full copy
        copySheet(new Cursor(srcWorkbook, srcSheet), new Cursor(destWorkbook, destSheet, formulas));
    }

    /**
     * Copy content from source sheet to destination sheet
     *
     * @param src  source
     * @param dest destination
     * @throws IOException if happened while copy
     */
    private static void copySheet(Cursor src, Cursor dest) throws IOException {
        dest.workbook.setSheetHidden(dest.getSheetIndex(), src.isSheetHidden());
        copyColumns(src.sheet, dest.sheet);
        for (var rowNum = src.sheet.getFirstRowNum(); rowNum <= src.sheet.getLastRowNum(); rowNum++) {
            if (rowNum < 0) {
                break;
            }
            src.row = src.sheet.getRow(rowNum);
            if (!XlsSheetsMatcher.isNullOrEmpty(src.row)) {
                dest.row = dest.sheet.createRow(rowNum);
                copyRow(src, dest);
            } else if (src.row != null && (src.row.getZeroHeight() || XlsSheetMerger.customHeight(src.row) >= 0)) {
                // an empty row keeps the height a user set for it, and whether it is hidden
                var row = dest.sheet.createRow(rowNum);
                row.setHeight(src.row.getHeight());
                row.setZeroHeight(src.row.getZeroHeight());
            }
            dest.row = src.row = null;
        }
        restoreArrayFormulas(dest);
        replaceMergedRegions(src.sheet.getMergedRegions(), dest.sheet);
        copyDrawings(src, dest);
    }

    /**
     * Copies the default width of the columns and height of the rows, and the width and the visibility of every
     * column either sheet lays out.
     */
    private static void copyColumns(Sheet src, Sheet dest) {
        dest.setDefaultColumnWidth(src.getDefaultColumnWidth());
        dest.setDefaultRowHeight(src.getDefaultRowHeight());
        var last = Math.max(XlsSheetMerger.lastLaidOutColumn(src), XlsSheetMerger.lastLaidOutColumn(dest));
        var columns = new TreeSet<Integer>();
        for (var column = 0; column <= last; column++) {
            if (XlsSheetMerger.columnLayout(dest, column) != XlsSheetMerger.columnLayout(src, column)) {
                columns.add(column);
            }
        }
        XlsSheetMerger.takeColumns(src, dest, columns);
    }

    /**
     * Replaces the merged regions of the destination sheet with the given ones.
     *
     * @param regions the merged regions the destination sheet gets
     * @param dest    destination sheet
     */
    static void replaceMergedRegions(List<CellRangeAddress> regions, Sheet dest) {
        // for some reason removing of merge regions doesn't work with normal loop
        for (var i = dest.getNumMergedRegions() - 1; i >= 0; i--) {
            dest.removeMergedRegion(i);
        }
        for (CellRangeAddress mergedRegion : regions) {
            dest.addMergedRegion(mergedRegion);
        }
    }

    /**
     * Removes all rows from sheet, its array formulas first: a row holding a part of an array formula cannot be
     * removed.
     *
     * @param sheet sheet
     */
    private static void removeAllRows(Sheet sheet) {
        for (var row : sheet) {
            for (var cell : row) {
                if (cell.isPartOfArrayFormulaGroup()) {
                    sheet.removeArrayFormula(cell);
                }
            }
        }
        for (var rowNum = sheet.getFirstRowNum(); rowNum <= sheet.getLastRowNum(); rowNum++) {
            var row = sheet.getRow(rowNum);
            if (row != null) {
                sheet.removeRow(row);
            }
        }
    }

    /**
     * Copy row from source sheet to dest sheet
     *
     * @param src  source
     * @param dest destination
     */
    private static void copyRow(Cursor src, Cursor dest) {
        dest.row.setHeight(src.row.getHeight());
        dest.row.setZeroHeight(src.row.getZeroHeight());
        for (var cellNum = src.row.getFirstCellNum(); cellNum <= src.row.getLastCellNum(); cellNum++) {
            if (cellNum < 0) {
                break;
            }
            src.cell = src.row.getCell(cellNum);
            if (src.cell != null) {
                dest.cell = dest.row.createCell(cellNum);
                copyCell(src, dest);
                dest.cellStyle = src.cellStyle = null;
            }
            dest.cell = src.cell = null;
        }
    }

    /**
     * Copy cell from source sheet to dest sheet
     *
     * @param src  source
     * @param dest destination
     */
    private static void copyCell(Cursor src, Cursor dest) {
        copyStyles(src, dest);
        copyValue(src, dest);
        copyCommentOf(src, dest);
    }

    /**
     * Copies the value or the formula of a cell. The destination cell keeps its style and comment.
     *
     * <p>A formula comes with the value it keeps, which it goes on showing when it cannot be recalculated, such as a
     * formula that reads another workbook. An array formula is written as a plain formula in its first cell, which
     * notes the cells the formula spans where they land, for {@link #restoreArrayFormulas(Cursor)}; the other cells it
     * spans get the values it gave them. A cell written over the first cell of an array formula copied before is no
     * longer the first cell of that array formula.
     *
     * @param src  source
     * @param dest destination
     * @throws LinkedWorkbookMismatchException when the formula reads another workbook through a link the workbook of
     *                                         the destination numbers differently; a link it lacks is brought along
     */
    static void copyValue(Cursor src, Cursor dest) {
        dest.arrayFormulas.removeIf(range -> range.getFirstRow() == dest.cell.getRowIndex()
                && range.getFirstColumn() == dest.cell.getColumnIndex());
        switch (src.cell.getCellType()) {
            case BLANK -> dest.cell.setBlank();
            case STRING -> dest.cell.setCellValue(src.cell.getRichStringCellValue());
            case ERROR -> dest.cell.setCellErrorValue(src.cell.getErrorCellValue());
            case BOOLEAN -> dest.cell.setCellValue(src.cell.getBooleanCellValue());
            case NUMERIC -> dest.cell.setCellValue(src.cell.getNumericCellValue());
            case FORMULA -> {
                if (src.cell.isPartOfArrayFormulaGroup() && !startsArrayFormula(src.cell)) {
                    valueOf(src.cell, src.cell.getCachedFormulaResultType()).accept(dest.cell);
                    // the cell is a part of the array formula once it is restored
                    dest.addFormulaCell(dest.cell);
                    return;
                }
                XlsExternalLinks.linkWorkbooks(src.cell, dest.cell);
                dest.cell.setCellFormula(src.cell.getCellFormula());
                valueOf(src.cell, src.cell.getCachedFormulaResultType()).accept(dest.cell);
                dest.addFormulaCell(dest.cell);
                if (startsArrayFormula(src.cell)) {
                    var range = src.cell.getArrayFormulaRange();
                    var rows = dest.cell.getRowIndex() - src.cell.getRowIndex();
                    var columns = dest.cell.getColumnIndex() - src.cell.getColumnIndex();
                    dest.arrayFormulas.add(new CellRangeAddress(range.getFirstRow() + rows,
                            range.getLastRow() + rows,
                            range.getFirstColumn() + columns,
                            range.getLastColumn() + columns));
                }
            }
            default -> throw new IllegalStateException("Unexpected cell type: " + src.cell.getCellType());
        }
    }

    private static boolean startsArrayFormula(Cell cell) {
        if (!cell.isPartOfArrayFormulaGroup()) {
            return false;
        }
        var range = cell.getArrayFormulaRange();
        return range.getFirstRow() == cell.getRowIndex() && range.getFirstColumn() == cell.getColumnIndex();
    }

    /**
     * Makes the array formulas copied into a sheet array formulas again, once every cell they span is written: the
     * first cell keeps the formula, and the other cells keep the values the formula gave them.
     *
     * <p>An array formula whose first cell no longer holds a formula, or that crosses an array formula restored
     * before, was written over later, and is left as it was written.
     */
    static void restoreArrayFormulas(Cursor dest) {
        dest.arrayFormulas.stream()
                .filter(range -> isWhole(dest.sheet, range))
                .forEach(range -> rewriteArray(dest.sheet,
                        range,
                        formula -> dest.sheet.setArrayFormula(formula, range)));
        dest.arrayFormulas.clear();
    }

    private static boolean isWhole(Sheet sheet, CellRangeAddress range) {
        var first = SheetUtil.getCell(sheet, range.getFirstRow(), range.getFirstColumn());
        if (first == null || first.getCellType() != CellType.FORMULA) {
            return false;
        }
        for (var address : range) {
            var cell = SheetUtil.getCell(sheet, address.getRow(), address.getColumn());
            if (cell != null && cell.isPartOfArrayFormulaGroup()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Turns the array formula a cell belongs to into plain content, so that each cell it spans can be written: the
     * first cell keeps the formula as a plain one, and the other cells keep the values the formula gave them.
     *
     * <p>A cell of no array formula is left as it is.
     */
    static void releaseArrayFormula(Cell cell) {
        if (!cell.isPartOfArrayFormulaGroup()) {
            return;
        }
        var sheet = cell.getSheet();
        var range = cell.getArrayFormulaRange();
        rewriteArray(sheet, range, formula -> {
            sheet.removeArrayFormula(cell);
            cellAt(sheet, new CellAddress(range.getFirstRow(), range.getFirstColumn())).setCellFormula(formula);
        });
    }

    /**
     * Changes how the cells of a range hold an array formula, and keeps what they show: the change gets the formula of
     * the first cell, and every cell gets back the value it showed, which an .xls workbook resets.
     */
    private static void rewriteArray(Sheet sheet, CellRangeAddress range, Consumer<String> change) {
        var formula = cellAt(sheet, new CellAddress(range.getFirstRow(), range.getFirstColumn())).getCellFormula();
        var values = new LinkedHashMap<CellAddress, Consumer<Cell>>();
        for (var address : range) {
            var cell = SheetUtil.getCell(sheet, address.getRow(), address.getColumn());
            if (cell != null) {
                values.put(address, valueOf(cell, cell.getCellType() == CellType.FORMULA
                        ? cell.getCachedFormulaResultType()
                        : cell.getCellType()));
            }
        }
        change.accept(formula);
        values.forEach((address, value) -> value.accept(cellAt(sheet, address)));
    }

    private static Cell cellAt(Sheet sheet, CellAddress address) {
        return CellUtil.getCell(CellUtil.getRow(address.getRow(), sheet), address.getColumn());
    }

    /**
     * Writes into another cell the value a cell holds as the given type: the type of a cell with a value, or the type
     * of the value a formula cell keeps.
     */
    private static Consumer<Cell> valueOf(Cell cell, CellType type) {
        return switch (type) {
            case NUMERIC -> {
                var value = cell.getNumericCellValue();
                yield dest -> dest.setCellValue(value);
            }
            case STRING -> {
                var value = cell.getRichStringCellValue();
                yield dest -> dest.setCellValue(value);
            }
            case BOOLEAN -> {
                var value = cell.getBooleanCellValue();
                yield dest -> dest.setCellValue(value);
            }
            case ERROR -> {
                var value = cell.getErrorCellValue();
                yield dest -> dest.setCellErrorValue(value);
            }
            default -> dest -> {
                // no value
            };
        };
    }

    /**
     * Copies the comment of a cell, or removes the comment of the destination cell when the source has none.
     *
     * @param src  source
     * @param dest destination
     */
    static void copyCommentOf(Cursor src, Cursor dest) {
        src.comment = src.cell.getCellComment();
        if (src.comment != null) {
            copyComment(src, dest);
            src.comment = null;
            dest.comment = null;
        } else if (dest.cell.getCellComment() != null) {
            dest.cell.removeCellComment();
        }
    }

    /**
     * Copy cell styles from source sheet to dest sheet
     *
     * @param src  source
     * @param dest destination
     */
    static void copyStyles(Cursor src, Cursor dest) {
        src.cellStyle = src.cell.getCellStyle();
        dest.cellStyle = dest.cell.getCellStyle();
        if (XlsSheetsMatcher.equalStylesInCell(src, dest)) {
            // nothing to copy
            return;
        }
        var numCellStyles = dest.workbook.getNumCellStyles();
        for (var i = 0; i < numCellStyles; i++) {
            dest.cellStyle = dest.workbook.getCellStyleAt(i);
            if (XlsSheetsMatcher.equalStylesInCell(src, dest)) {
                // reuse cell styles
                dest.cell.setCellStyle(dest.cellStyle);
                return;
            }
        }
        dest.cellStyle = dest.workbook.createCellStyle();

        dest.cellStyle.cloneStyleFrom(src.cellStyle);
        dest.cellStyle.setDataFormat(src.cellStyle.getDataFormat());

        if (dest.cellStyle instanceof XSSFCellStyle style) {
            var coreCtxf = style.getCoreXf();
            var styleId = coreCtxf.getXfId();
            var stylesSource = ((XSSFWorkbook) dest.originalWorkbook()).getStylesSource();
            if (stylesSource.getCellStyleXfAt((int) styleId) == null) {
                // if parent style doesn't exist in the table, just reset it
                coreCtxf.setXfId(0);
            }
        }

        dest.cell.setCellStyle(dest.cellStyle);
    }

    /**
     * Copy cell comment from source sheet to dest sheet
     *
     * @param src  source
     * @param dest destination
     */
    private static void copyComment(Cursor src, Cursor dest) {
        var srcClientAnchor = src.comment.getClientAnchor();
        var version = dest.workbook.getSpreadsheetVersion();
        var box = areaOf(srcClientAnchor);
        // a box next to a cell moved to the edge of the sheet would leave it: it stays inside, at the edge
        var rows = shift(dest.cell.getRowIndex() - src.cell.getRowIndex(),
                -box.getFirstRow(),
                version.getLastRowIndex() - box.getLastRow());
        var columns = shift(dest.cell.getColumnIndex() - src.cell.getColumnIndex(),
                -box.getFirstColumn(),
                version.getLastColumnIndex() - box.getLastColumn());
        if (dest.cell.getCellComment() != null) {
            dest.comment = dest.cell.getCellComment();
            var destClientAnchor = dest.comment.getClientAnchor();
            copyClientAnchor(srcClientAnchor, destClientAnchor, rows, columns);
        } else {
            // Apache POI doesn't support properly threaded comments
            // see https://bz.apache.org/bugzilla/show_bug.cgi?id=65462
            // So they may coppied incorrectly
            var creationHelper = dest.workbook.getCreationHelper();
            var destClientAnchor = creationHelper.createClientAnchor();
            // raw comment initialization without resize
            destClientAnchor.setRow1(dest.cell.getRowIndex());
            destClientAnchor.setRow2(dest.cell.getRowIndex());
            destClientAnchor.setCol1(dest.cell.getColumnIndex());
            destClientAnchor.setCol2(dest.cell.getColumnIndex());
            Drawing<?> drawingPatriarch = dest.sheet.createDrawingPatriarch();
            dest.comment = drawingPatriarch.createCellComment(destClientAnchor);

            // set comment position and resize
            copyClientAnchor(srcClientAnchor, destClientAnchor, rows, columns);
            dest.cell.setCellComment(dest.comment);
        }

        dest.comment.setAuthor(getCommentAuthor(src.comment));
        dest.comment.setString(src.comment.getString());
    }

    /**
     * The shift of a box by its cell, kept so the box stays inside the sheet. A box that already reaches past the
     * edge of the sheet only moves towards it.
     */
    private static int shift(int by, int least, int most) {
        return by == 0 ? 0 : Math.clamp(by, least, Math.max(least, most));
    }

    /**
     * The cells a box anchored to a sheet lies over.
     */
    static CellRangeAddress areaOf(ClientAnchor anchor) {
        return new CellRangeAddress(Math.min(anchor.getRow1(), anchor.getRow2()),
                Math.max(anchor.getRow1(), anchor.getRow2()),
                Math.min(anchor.getCol1(), anchor.getCol2()),
                Math.max(anchor.getCol1(), anchor.getCol2()));
    }

    /**
     * Copies the place of a box moved by rows and columns, so a comment copied to another cell stays next to it.
     */
    private static void copyClientAnchor(ClientAnchor src, ClientAnchor dest, int rows, int columns) {
        dest.setCol1(src.getCol1() + columns);
        dest.setCol2(src.getCol2() + columns);
        dest.setRow1(src.getRow1() + rows);
        dest.setRow2(src.getRow2() + rows);
        dest.setDx1(src.getDx1());
        dest.setDx2(src.getDx2());
        dest.setDy1(src.getDy1());
        dest.setDy2(src.getDy2());
        dest.setAnchorType(src.getAnchorType());
    }

    /**
     * Copy drawings from source sheet to dest sheet
     *
     * @param src  source
     * @param dest destination
     */
    static void copyDrawings(Cursor src, Cursor dest) throws IOException {
        if (!(src.sheet instanceof XSSFSheet) || !(dest.sheet instanceof XSSFSheet)) {
            return;
        }

        var srcDrawing = (XSSFDrawing) src.sheet.getDrawingPatriarch();
        var destDrawing = (XSSFDrawing) dest.sheet.getDrawingPatriarch();
        if (srcDrawing == null) {
            if (destDrawing != null) {
                deletePictures(destDrawing);
            }
            return;
        }

        Function<XSSFDrawing, Map<String, XSSFPicture>> pictureGroup = drawing -> drawing.getShapes()
                .stream()
                .filter(XSSFPicture.class::isInstance)
                .map(XSSFPicture.class::cast)
                .collect(Collectors.toMap(XSSFPicture::getShapeName, Function.identity()));

        var pictures1 = pictureGroup.apply(srcDrawing);
        if (destDrawing == null) {
            for (XSSFPicture srcPicture : pictures1.values()) {
                src.picture = srcPicture;
                createPicture(src, dest);
                src.picture = null;
            }
        } else {
            var pictures2 = pictureGroup.apply(destDrawing);
            replacePictures(src, dest, pictures1, pictures2);
        }
    }

    private static void deletePictures(XSSFDrawing drawing) {
        for (Shape shape : drawing.getShapes()) {
            if (shape instanceof XSSFPicture picture) {
                deletePicture(picture);
            }
        }
    }

    /**
     * Makes the destination pictures match the source ones: rewrites the data of the pictures present in both, creates
     * the missing ones and deletes the pictures the source does not have.
     */
    private static void replacePictures(Cursor src,
                                        Cursor dest,
                                        Map<String, XSSFPicture> pictures1,
                                        Map<String, XSSFPicture> pictures2) throws IOException {
        for (Map.Entry<String, XSSFPicture> entry : pictures1.entrySet()) {
            src.picture = entry.getValue();
            var destPic = pictures2.get(entry.getKey());
            if (destPic != null) {
                try (var os = destPic.getPictureData().getPackagePart().getOutputStream()) {
                    os.write(src.picture.getPictureData().getData());
                }
            } else {
                createPicture(src, dest);
            }
            src.picture = null;
        }

        for (Map.Entry<String, XSSFPicture> entry : pictures2.entrySet()) {
            if (pictures1.get(entry.getKey()) == null) {
                deletePicture(entry.getValue());
            }
        }
    }

    /**
     * Delete picture from sheet
     *
     * @param picture to delete
     */
    private static void deletePicture(XSSFPicture picture) {
        var id = Optional.ofNullable(picture.getCTPicture())
                .map(CTPicture::getBlipFill)
                .map(CTBlipFillProperties::getBlip)
                .map(CTBlip::getEmbed)
                .orElse(null);
        var drawing = picture.getDrawing();
        if (id != null) {
            var packagePart = drawing.getPackagePart();
            packagePart.removeRelationship(id);
            packagePart.getPackage()
                    .deletePartRecursive(drawing.getRelationPartById(id).getDocumentPart().getPackagePart().getPartName());
        }

        try (var cursor = picture.getCTPicture().newCursor()) {
            cursor.toParent();
            if (cursor.getObject() instanceof CTTwoCellAnchor) {
                var i = indexOf(drawing.getCTDrawing().getTwoCellAnchorArray(), cursor.getObject());
                if (i >= 0) {
                    drawing.getCTDrawing().removeTwoCellAnchor(i);
                }
            } else if (cursor.getObject() instanceof CTOneCellAnchor) {
                var i = indexOf(drawing.getCTDrawing().getOneCellAnchorArray(), cursor.getObject());
                if (i >= 0) {
                    drawing.getCTDrawing().removeOneCellAnchor(i);
                }
            } else if (cursor.getObject() instanceof CTAbsoluteAnchor) {
                var i = indexOf(drawing.getCTDrawing().getAbsoluteAnchorArray(), cursor.getObject());
                if (i >= 0) {
                    drawing.getCTDrawing().removeAbsoluteAnchor(i);
                }
            }
        }
    }

    /**
     * Finds the position of the anchor among the anchors of a drawing.
     *
     * @return the index of the anchor, or {@code -1} if the drawing does not hold it
     */
    private static int indexOf(XmlObject[] anchors, XmlObject anchor) {
        for (var i = 0; i < anchors.length; i++) {
            if (anchor.equals(anchors[i])) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Copy picture from source sheet to dest sheet
     *
     * @param src  source
     * @param dest destination
     */
    private static void createPicture(Cursor src, Cursor dest) {
        var creationHelper = dest.workbook.getCreationHelper();
        var srcClientAnchor = src.picture.getClientAnchor();
        var destClientAnchor = creationHelper.createClientAnchor();
        copyClientAnchor(srcClientAnchor, destClientAnchor, 0, 0);

        var srcPictureData = src.picture.getPictureData();
        var pictureIdx = dest.workbook.addPicture(srcPictureData.getData(), srcPictureData.getPictureType());
        Drawing<?> destDrawing = dest.sheet.getDrawingPatriarch();
        if (destDrawing == null) {
            destDrawing = dest.sheet.createDrawingPatriarch();
        }
        destDrawing.createPicture(destClientAnchor, pictureIdx);
    }

}
