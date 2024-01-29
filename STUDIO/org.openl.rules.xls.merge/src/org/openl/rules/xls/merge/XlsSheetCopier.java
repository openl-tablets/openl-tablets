package org.openl.rules.xls.merge;

import static org.openl.rules.xls.merge.XlsSheetsMatcher.getCommentAuthor;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Shape;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFPicture;
import org.apache.poi.xssf.usermodel.XSSFPictureData;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.xmlbeans.XmlCursor;
import org.openxmlformats.schemas.drawingml.x2006.main.CTBlip;
import org.openxmlformats.schemas.drawingml.x2006.main.CTBlipFillProperties;
import org.openxmlformats.schemas.drawingml.x2006.spreadsheetDrawing.CTAbsoluteAnchor;
import org.openxmlformats.schemas.drawingml.x2006.spreadsheetDrawing.CTOneCellAnchor;
import org.openxmlformats.schemas.drawingml.x2006.spreadsheetDrawing.CTPicture;
import org.openxmlformats.schemas.drawingml.x2006.spreadsheetDrawing.CTTwoCellAnchor;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTXf;

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
     * @param srcWorkbook source workbook
     * @param srcSheet source sheet
     * @param destWorkbook destination workbook
     * @param destSheet destination sheet
     * @param formulas formulas to evaluate
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
     * @param src source
     * @param dest destination
     * @throws IOException if happened while copy
     */
    private static void copySheet(Cursor src, Cursor dest) throws IOException {
        dest.workbook.setSheetHidden(dest.getSheetIndex(), src.isSheetHidden());
        for (int rowNum = src.sheet.getFirstRowNum(); rowNum <= src.sheet.getLastRowNum(); rowNum++) {
            if (rowNum < 0) {
                break;
            }
            src.row = src.sheet.getRow(rowNum);
            if (!XlsSheetsMatcher.isNullOrEmpty(src.row)) {
                dest.row = dest.sheet.createRow(rowNum);
                copyRow(src, dest);
            }
            dest.row = src.row = null;
        }
        for (CellRangeAddress mergedRegion : src.sheet.getMergedRegions()) {
            dest.sheet.addMergedRegion(mergedRegion);
        }
        copyDrawings(src, dest);
    }

    /**
     * Removes all rows and merged regions from sheet
     *
     * @param sheet sheet
     */
    private static void removeAllRows(Sheet sheet) {
        // for some reason removing of merge regions doesn't work with normal loop
        for (int i = sheet.getNumMergedRegions() - 1; i >= 0; i--) {
            sheet.removeMergedRegion(i);
        }
        for (int rowNum = sheet.getFirstRowNum(); rowNum <= sheet.getLastRowNum(); rowNum++) {
            Row row = sheet.getRow(rowNum);
            if (row != null) {
                sheet.removeRow(row);
            }
        }
    }

    /**
     * Copy row from source sheet to dest sheet
     *
     * @param src source
     * @param dest destination
     */
    private static void copyRow(Cursor src, Cursor dest) {
        dest.row.setHeight(src.row.getHeight());
        for (int cellNum = src.row.getFirstCellNum(); cellNum <= src.row.getLastCellNum(); cellNum++) {
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
     * @param src source
     * @param dest destination
     */
    private static void copyCell(Cursor src, Cursor dest) {
        dest.sheet.setColumnWidth(dest.cell.getColumnIndex(), src.sheet.getColumnWidth(src.cell.getColumnIndex()));
        copyStyles(src, dest);
        switch (src.cell.getCellType()) {
            case BLANK:
                dest.cell.setBlank();
                break;
            case STRING:
                dest.cell.setCellValue(src.cell.getRichStringCellValue());
                break;
            case ERROR:
                dest.cell.setCellErrorValue(src.cell.getErrorCellValue());
                break;
            case BOOLEAN:
                dest.cell.setCellValue(src.cell.getBooleanCellValue());
                break;
            case NUMERIC:
                dest.cell.setCellValue(src.cell.getNumericCellValue());
                break;
            case FORMULA:
                dest.cell.setCellFormula(src.cell.getCellFormula());
                dest.addFormulaCell(dest.cell);
                break;
            default:
                throw new IllegalStateException("Unexpected cell type: " + src.cell.getCellType());
        }

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
     * @param src source
     * @param dest destination
     */
    private static void copyStyles(Cursor src, Cursor dest) {
        src.cellStyle = src.cell.getCellStyle();
        dest.cellStyle = dest.cell.getCellStyle();
        if (XlsSheetsMatcher.equalStylesInCell(src, dest)) {
            // nothing to copy
            return;
        }
        int numCellStyles = dest.workbook.getNumCellStyles();
        for (int i = 0; i < numCellStyles; i++) {
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

        if (dest.cellStyle instanceof XSSFCellStyle) {
            CTXf coreCtxf = ((XSSFCellStyle) dest.cellStyle).getCoreXf();
            long styleId = coreCtxf.getXfId();
            StylesTable stylesSource = ((XSSFWorkbook) dest.originalWorkbook()).getStylesSource();
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
     * @param src source
     * @param dest destination
     */
    private static void copyComment(Cursor src, Cursor dest) {
        ClientAnchor srcClientAnchor = src.comment.getClientAnchor();
        if (dest.cell.getCellComment() != null) {
            dest.comment = dest.cell.getCellComment();
            ClientAnchor destClientAnchor = dest.comment.getClientAnchor();
            copyClientAnchor(srcClientAnchor, destClientAnchor);
        } else {
            // Apache POI doesn't support properly threaded comments
            // see https://bz.apache.org/bugzilla/show_bug.cgi?id=65462
            // So they may coppied incorrectly
            CreationHelper creationHelper = dest.workbook.getCreationHelper();
            ClientAnchor destClientAnchor = creationHelper.createClientAnchor();
            // raw comment initialization without resize
            destClientAnchor.setRow1(dest.cell.getRowIndex());
            destClientAnchor.setRow2(dest.cell.getRowIndex());
            destClientAnchor.setCol1(dest.cell.getColumnIndex());
            destClientAnchor.setCol2(dest.cell.getColumnIndex());
            Drawing<?> drawingPatriarch = dest.sheet.createDrawingPatriarch();
            dest.comment = drawingPatriarch.createCellComment(destClientAnchor);

            // set comment position and resize
            copyClientAnchor(srcClientAnchor, destClientAnchor);
            dest.cell.setCellComment(dest.comment);
        }

        dest.comment.setAuthor(getCommentAuthor(src.comment));
        dest.comment.setString(src.comment.getString());
    }

    /**
     * Copy client anchor
     *
     * @param src source
     * @param dest destination
     */
    private static void copyClientAnchor(ClientAnchor src, ClientAnchor dest) {
        dest.setCol1(src.getCol1());
        dest.setCol2(src.getCol2());
        dest.setRow1(src.getRow1());
        dest.setRow2(src.getRow2());
        dest.setDx1(src.getDx1());
        dest.setDx2(src.getDx2());
        dest.setDy1(src.getDy1());
        dest.setDy2(src.getDy2());
        dest.setAnchorType(src.getAnchorType());
    }

    /**
     * Copy drawings from source sheet to dest sheet
     *
     * @param src source
     * @param dest destination
     */
    private static void copyDrawings(Cursor src, Cursor dest) throws IOException {
        if (!(src.sheet instanceof XSSFSheet) || !(dest.sheet instanceof XSSFSheet)) {
            return;
        }

        XSSFDrawing srcDrawing = (XSSFDrawing) src.sheet.getDrawingPatriarch();
        XSSFDrawing destDrawing = (XSSFDrawing) dest.sheet.getDrawingPatriarch();
        if (srcDrawing == null) {
            if (destDrawing != null) {
                for (Shape shape : destDrawing.getShapes()) {
                    if (shape instanceof XSSFPicture) {
                        deletePicture((XSSFPicture) shape);
                    }
                }
            }
            return;
        }

        Function<XSSFDrawing, Map<String, XSSFPicture>> pictureGroup = drawing -> drawing.getShapes()
            .stream()
            .filter(XSSFPicture.class::isInstance)
            .map(XSSFPicture.class::cast)
            .collect(Collectors.toMap(XSSFPicture::getShapeName, Function.identity()));

        Map<String, XSSFPicture> pictures1 = pictureGroup.apply(srcDrawing);
        if (destDrawing == null) {
            for (XSSFPicture srcPicture : pictures1.values()) {
                src.picture = srcPicture;
                createPicture(src, dest);
                src.picture = null;
            }
        } else {
            Map<String, XSSFPicture> pictures2 = pictureGroup.apply(destDrawing);
            for (Map.Entry<String, XSSFPicture> entry : pictures1.entrySet()) {
                src.picture = entry.getValue();
                XSSFPicture destPic = pictures2.get(entry.getKey());
                if (destPic != null) {
                    try (OutputStream os = destPic.getPictureData().getPackagePart().getOutputStream()) {
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
    }

    /**
     * Delete picture from sheet
     *
     * @param picture to delete
     */
    private static void deletePicture(XSSFPicture picture) {
        String id = Optional.ofNullable(picture.getCTPicture())
            .map(CTPicture::getBlipFill)
            .map(CTBlipFillProperties::getBlip)
            .map(CTBlip::getEmbed)
            .orElse(null);
        XSSFDrawing drawing = picture.getDrawing();
        if (id != null) {
            PackagePart packagePart = drawing.getPackagePart();
            packagePart.removeRelationship(id);
            packagePart.getPackage()
                .deletePartRecursive(drawing.getRelationPartById(id).getDocumentPart().getPackagePart().getPartName());
        }

        try (XmlCursor cursor = picture.getCTPicture().newCursor()) {
            cursor.toParent();
            if (cursor.getObject() instanceof CTTwoCellAnchor) {
                int i = 0;
                for (CTTwoCellAnchor anchor : drawing.getCTDrawing().getTwoCellAnchorArray()) {
                    if (cursor.getObject().equals(anchor)) {
                        drawing.getCTDrawing().removeTwoCellAnchor(i);
                        break;
                    }
                    i++;
                }
            } else if (cursor.getObject() instanceof CTOneCellAnchor) {
                int i = 0;
                for (CTOneCellAnchor anchor : drawing.getCTDrawing().getOneCellAnchorArray()) {
                    if (cursor.getObject().equals(anchor)) {
                        drawing.getCTDrawing().removeOneCellAnchor(i);
                        break;
                    }
                    i++;
                }
            } else if (cursor.getObject() instanceof CTAbsoluteAnchor) {
                int i = 0;
                for (CTAbsoluteAnchor anchor : drawing.getCTDrawing().getAbsoluteAnchorArray()) {
                    if (cursor.getObject().equals(anchor)) {
                        drawing.getCTDrawing().removeAbsoluteAnchor(i);
                        break;
                    }
                    i++;
                }
            }
        }
    }

    /**
     * Copy picture from source sheet to dest sheet
     *
     * @param src source
     * @param dest destination
     */
    private static void createPicture(Cursor src, Cursor dest) {
        CreationHelper creationHelper = dest.workbook.getCreationHelper();
        ClientAnchor srcClientAnchor = src.picture.getClientAnchor();
        ClientAnchor destClientAnchor = creationHelper.createClientAnchor();
        copyClientAnchor(srcClientAnchor, destClientAnchor);

        XSSFPictureData srcPictureData = src.picture.getPictureData();
        int pictureIdx = dest.workbook.addPicture(srcPictureData.getData(), srcPictureData.getPictureType());
        Drawing<?> destDrawing = dest.sheet.getDrawingPatriarch();
        if (destDrawing == null) {
            destDrawing = dest.sheet.createDrawingPatriarch();
        }
        destDrawing.createPicture(destClientAnchor, pictureIdx);
    }

}
