package org.openl.rules.xls.merge;

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.BuiltinFormats;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Color;
import org.apache.poi.ss.usermodel.Comment;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.RichTextString;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Shape;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFPicture;
import org.apache.poi.xssf.usermodel.XSSFPictureData;
import org.apache.poi.xssf.usermodel.XSSFShape;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openl.util.CollectionUtils;

/**
 * Finds changes in two sheets from two workbooks.
 *
 * @author Vladyslav Pikus
 */
public class XlsSheetsMatcher {

    private static final Logger LOG = LoggerFactory.getLogger(XlsSheetsMatcher.class);

    private static final Pattern THREADED_COMMENT_MARKER = Pattern
        .compile("^tc=\\{[0-9a-fA-F]{8}-(?:[0-9a-fA-F]{4}-){3}[0-9a-fA-F]{12}}$");

    /**
     * Finds changes in two sheets from two workbooks.
     *
     * @param baseWorkbook base workbook
     * @param baseSheet sheet of {@code baseWorkbook}
     * @param workbook updated workbook
     * @param sheet sheet of {@code workbook}
     * @return {@code true} if any difference is detected between {@code baseSheet} and {@code sheet}, otherwise
     *         {@code false}
     */
    public static boolean hasChanges(Workbook baseWorkbook, Sheet baseSheet, Workbook workbook, Sheet sheet) {
        Cursor baseCursor = new Cursor(baseWorkbook, baseSheet);
        Cursor cursor = new Cursor(workbook, sheet);

        return !equalSheets(baseCursor, cursor);
    }

    /**
     * Equals two sheets from two workbooks.
     *
     * @param baseCursor base cursor
     * @param cursor second cursor
     * @return {@code true} if no changes is detected, otherwise {@code false}
     */
    private static boolean equalSheets(Cursor baseCursor, Cursor cursor) {
        // number of rows and cells equal for base sheet and second one, let's check content
        boolean isHidden1 = baseCursor.isSheetHidden();
        boolean isHidden2 = cursor.isSheetHidden();
        if (isHidden1 != isHidden2) {
            LOG.debug("Base '{}' sheet is hidden={}, but another sheet is hidden={}.",
                baseCursor.sheet.getSheetName(),
                isHidden1,
                isHidden2);
            return false;
        }

        int lastRowNum = Math.max(baseCursor.sheet.getLastRowNum(), cursor.sheet.getLastRowNum());
        int firstRowNum = Math.min(baseCursor.sheet.getFirstRowNum(), cursor.sheet.getFirstRowNum());
        for (int i = firstRowNum; i <= lastRowNum; i++) {
            baseCursor.row = baseCursor.sheet.getRow(i);
            cursor.row = cursor.sheet.getRow(i);
            if (!equalContentInRow(baseCursor, cursor)) {
                return false;
            }
        }

        return equalDrawings(baseCursor, cursor);
    }

    /**
     * Equals drawings elements from two sheets
     *
     * @param baseCursor base cursor
     * @param cursor second cursor
     * @return {@code true} if no changes is detected, otherwise {@code false}
     */
    private static boolean equalDrawings(Cursor baseCursor, Cursor cursor) {
        List<XSSFPicture> shapes1 = baseCursor.getSheetPictures();
        List<XSSFPicture> shapes2 = cursor.getSheetPictures();
        if (CollectionUtils.isEmpty(shapes1)) {
            if (CollectionUtils.isNotEmpty(shapes2)) {
                LOG.debug("Base '{}' sheet hasn't drawings, but another sheet has", baseCursor.sheet.getSheetName());
                return false;
            } else {
                return true;
            }
        }
        if (CollectionUtils.isEmpty(shapes2)) {
            LOG.debug("Base '{}' sheet has drawings, but another sheet hasn't", baseCursor.sheet.getSheetName());
            return false;
        }
        if (shapes1.size() != shapes2.size()) {
            LOG.debug("Base '{}' sheet has {} drawings, but another sheet has {}",
                baseCursor.sheet.getSheetName(),
                shapes1.size(),
                shapes2.size());
            return false;
        }
        Map<String, XSSFShape> shapesMap2 = cursor.getSheetShapes()
            .stream()
            .collect(Collectors.toMap(Shape::getShapeName, Function.identity()));
        for (XSSFPicture picture1 : shapes1) {
            XSSFPicture picture2 = Optional.ofNullable(shapesMap2.get(picture1.getShapeName()))
                .filter(XSSFPicture.class::isInstance)
                .map(XSSFPicture.class::cast)
                .orElse(null);
            if (picture2 == null) {
                LOG.debug("Base '{}' sheet has picture '{}', but another sheet hasn't",
                    baseCursor.sheet.getSheetName(),
                    picture1.getShapeName());
                return false;
            }

            XSSFPictureData pictureData1 = picture1.getPictureData();
            XSSFPictureData pictureData2 = picture2.getPictureData();

            int pictureType1 = pictureData1.getPictureType();
            int pictureType2 = pictureData2.getPictureType();
            if (pictureType1 != pictureType2) {
                LOG.debug("Base '{}' sheet, picture '{}' PictureType={}, but another PictureType={}",
                    baseCursor.sheet.getSheetName(),
                    picture1.getShapeName(),
                    pictureType1,
                    pictureType2);
                return false;
            }
            String mimeType1 = pictureData1.getMimeType();
            String mimeType2 = pictureData2.getMimeType();
            if (!mimeType1.equals(mimeType2)) {
                LOG.debug("Base '{}' sheet, picture '{}' MimeType={}, but another MimeType={}",
                    baseCursor.sheet.getSheetName(),
                    picture1.getShapeName(),
                    mimeType1,
                    mimeType2);
                return false;
            }
            byte[] data1 = pictureData1.getData();
            byte[] data2 = pictureData2.getData();
            if (data1.length != data2.length) {
                LOG.debug("Base '{}' sheet, picture '{}' data.length={}, but another data.length={}",
                    baseCursor.sheet.getSheetName(),
                    picture1.getShapeName(),
                    data1.length,
                    data2.length);
                return false;
            }
            for (int i = 0; i < data1.length; i++) {
                if (data1[i] != data2[i]) {
                    LOG.debug("Base '{}' sheet, picture '{}' data[{}]={}, but another data.[{}]={}",
                        baseCursor.sheet.getSheetName(),
                        picture1.getShapeName(),
                        i,
                        data1[i],
                        i,
                        data2[i]);
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Equals rows from two sheets
     *
     * @param baseCursor base cursor
     * @param cursor second cursor
     * @return {@code true} if no changes is detected, otherwise {@code false}
     */
    private static boolean equalContentInRow(Cursor baseCursor, Cursor cursor) {
        if (isNullOrEmpty(baseCursor.row)) {
            // if row has no cell, we assume that it's equal to null
            if (isNullOrEmpty(cursor.row)) {
                return true;
            } else {
                LOG.debug("Base '{}' sheet {} row is null. But the same row in another sheet is not.",
                    baseCursor.sheet.getSheetName(),
                    cursor.row.getRowNum());
                return false;
            }
        }
        if (isNullOrEmpty(cursor.row)) {
            LOG.debug("Base '{}' sheet {} row isn't null. But the same row in another sheet is null.",
                baseCursor.sheet.getSheetName(),
                baseCursor.row.getRowNum());
            return false;
        }

        int lastCellNum = Math.max(baseCursor.row.getLastCellNum(), cursor.row.getLastCellNum());
        int firstCellNum = Math.min(baseCursor.row.getFirstCellNum(), cursor.row.getFirstCellNum());
        for (int i = firstCellNum; i <= lastCellNum; i++) {
            baseCursor.cell = baseCursor.row.getCell(i);
            cursor.cell = cursor.row.getCell(i);
            if (!equalContentInCell(baseCursor, cursor)) {
                return false;
            }
        }
        return true;
    }

    public static boolean isNullOrEmpty(Row row) {
        if (row == null) {
            return true;
        }
        Iterator<Cell> cells = row.cellIterator();
        while (cells.hasNext()) {
            if (!isNullOrEmpty(cells.next())) {
                return false;
            }
        }
        // if row has no cell, assume that it's equal to null or no custom styles
        return true;
    }

    /**
     * Equals cells from two sheets
     *
     * @param baseCursor base cursor
     * @param cursor second cursor
     * @return {@code true} if no changes is detected, otherwise {@code false}
     */
    private static boolean equalContentInCell(Cursor baseCursor, Cursor cursor) {
        if (isNullOrEmpty(baseCursor.cell)) {
            if (isNullOrEmpty(cursor.cell)) {
                return true;
            } else {
                LOG.debug("Base sheet={}&cell={} is null, but second is not",
                    baseCursor.sheet.getSheetName(),
                    cursor.cell.getAddress());
                return false;
            }
        }
        if (isNullOrEmpty(cursor.cell)) {
            LOG.debug("Base sheet={}&cell={} isn't null, but second is null",
                baseCursor.sheet.getSheetName(),
                baseCursor.cell.getAddress());
            return false;
        }

        if (!equalCellMergedRegions(baseCursor, cursor)) {
            return false;
        }

        if (baseCursor.cell.getCellType() != cursor.cell.getCellType()) {
            LOG.debug("Base sheet={}&cell={} contentType='{}', but second contentType='{}'",
                baseCursor.sheet.getSheetName(),
                baseCursor.cell.getAddress(),
                baseCursor.cell.getCellType(),
                cursor.cell.getCellType());
            return false;
        }

        final CellType baseCellType = baseCursor.cell.getCellType();
        switch (baseCellType) {
            case BLANK:
            case STRING:
            case ERROR:
                if (!baseCursor.cell.toString().equals(cursor.cell.toString())) {
                    LOG.debug("Base sheet={}&cell={} string content='{}', but second string content='{}'",
                        baseCursor.sheet.getSheetName(),
                        baseCursor.cell.getAddress(),
                        baseCursor.cell.toString(),
                        cursor.cell.toString());
                    return false;
                }
                break;
            case BOOLEAN:
                if (baseCursor.cell.getBooleanCellValue() != cursor.cell.getBooleanCellValue()) {
                    LOG.debug("Base sheet={}&cell={} boolean content='{}', but another boolean content='{}'",
                        baseCursor.sheet.getSheetName(),
                        baseCursor.cell.getAddress(),
                        baseCursor.cell.getBooleanCellValue(),
                        cursor.cell.getBooleanCellValue());
                    return false;
                }
                break;
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(baseCursor.cell)) {
                    if (!DateUtil.isCellDateFormatted(cursor.cell)) {
                        LOG.debug("Base sheet={}&cell={} cell is date formatted, but another cell is not",
                            baseCursor.sheet.getSheetName(),
                            baseCursor.cell.getAddress());
                        return false;
                    }
                    Date date1 = baseCursor.cell.getDateCellValue();
                    Date date2 = cursor.cell.getDateCellValue();
                    if (!date1.equals(date2)) {
                        LOG.debug("Base sheet={}&cell={} date content='{}', but second date content='{}'",
                            baseCursor.sheet.getSheetName(),
                            baseCursor.cell.getAddress(),
                            date1,
                            date2);
                        return false;
                    }
                } else {
                    double num1 = baseCursor.cell.getNumericCellValue();
                    double num2 = cursor.cell.getNumericCellValue();
                    if (num1 != num2) {
                        LOG.debug("Base sheet={}&cell={} numeric content='{}', but second numeric content='{}'",
                            baseCursor.sheet.getSheetName(),
                            baseCursor.cell.getAddress(),
                            num1,
                            num2);
                        return false;
                    }
                }
                break;
            case FORMULA:
                // Trim leading/trailing spaces from formulas
                // For some unknown reason Apache POI org.apache.poi.ss.usermodel.Cell#setCellFormula trims spaces
                // automatically.As a result formula cell will not be equals if the same value is set via
                // Apache POI and Microsoft Office 360
                String formula1 = baseCursor.cell.getCellFormula().trim();
                String formula2 = cursor.cell.getCellFormula().trim();
                if (!formula1.equals(formula2)) {
                    LOG.debug("Base sheet={}&cell={} formula content='{}', but second formula content='{}'",
                        baseCursor.sheet.getSheetName(),
                        baseCursor.cell.getAddress(),
                        formula1,
                        formula2);
                    return false;
                }
                break;
            default:
                throw new IllegalStateException("Unexpected cell type: " + baseCellType);
        }

        baseCursor.comment = baseCursor.cell.getCellComment();
        cursor.comment = cursor.cell.getCellComment();
        if (!equalCommentInCell(baseCursor, cursor)) {
            LOG.debug("Base sheet={}&cell={} cell comment doesn't equal to another",
                baseCursor.sheet.getSheetName(),
                baseCursor.cell.getAddress());
            return false;
        }

        baseCursor.cellStyle = baseCursor.cell.getCellStyle();
        cursor.cellStyle = cursor.cell.getCellStyle();
        return equalStylesInCell(baseCursor, cursor);
    }

    /**
     * Equals merged regions from two sheets
     *
     * @param baseCursor base cursor
     * @param cursor second cursor
     * @return {@code true} if no changes is detected, otherwise {@code false}
     */
    private static boolean equalCellMergedRegions(Cursor baseCursor, Cursor cursor) {
        CellRangeAddress mergedRegion1 = baseCursor.getCellMergedRegion();
        CellRangeAddress mergedRegion2 = cursor.getCellMergedRegion();
        if (mergedRegion1 == null) {
            if (mergedRegion2 != null) {
                LOG.debug("Base sheet={}&cell={} is not merged, but second is merged",
                    baseCursor.sheet.getSheetName(),
                    baseCursor.cell.getAddress());
                return false;
            } else {
                return true;
            }
        }
        if (mergedRegion2 == null) {
            LOG.debug("Base sheet={}&cell={} is merged, but second is not",
                baseCursor.sheet.getSheetName(),
                baseCursor.cell.getAddress());
            return false;
        }

        int firstColumn1 = mergedRegion1.getFirstColumn();
        int firstColumn2 = mergedRegion2.getFirstColumn();
        if (firstColumn1 != firstColumn2) {
            LOG.debug("Base sheet={}&cell={} merged region FirstColumn='{}', but second merged region FirstColumn='{}'",
                baseCursor.sheet.getSheetName(),
                baseCursor.cell.getAddress(),
                firstColumn1,
                firstColumn2);
            return false;
        }

        int lastColumn1 = mergedRegion1.getLastColumn();
        int lastColumn2 = mergedRegion2.getLastColumn();
        if (lastColumn1 != lastColumn2) {
            LOG.debug("Base sheet={}&cell={} merged region LastColumn='{}', but second merged region LastColumn='{}'",
                baseCursor.sheet.getSheetName(),
                baseCursor.cell.getAddress(),
                lastColumn1,
                lastColumn2);
            return false;
        }

        int firstRow1 = mergedRegion1.getFirstRow();
        int firstRow2 = mergedRegion2.getFirstRow();
        if (firstRow1 != firstRow2) {
            LOG.debug("Base sheet={}&cell={} merged region FirstRow='{}', but second merged region FirstRow='{}'",
                baseCursor.sheet.getSheetName(),
                baseCursor.cell.getAddress(),
                firstRow1,
                firstRow2);
            return false;
        }

        int lastRow1 = mergedRegion1.getLastRow();
        int lastRow2 = mergedRegion2.getLastRow();
        if (lastRow1 != lastRow2) {
            LOG.debug("Base sheet={}&cell={} merged region LastRow='{}', but second merged region LastRow='{}'",
                baseCursor.sheet.getSheetName(),
                baseCursor.cell.getAddress(),
                lastRow1,
                lastRow2);
            return false;
        }

        return true;
    }

    /**
     * Equals cell comments
     *
     * @param baseCursor base cursor
     * @param cursor second cursor
     * @return {@code true} if no changes is detected, otherwise {@code false}
     */
    private static boolean equalCommentInCell(Cursor baseCursor, Cursor cursor) {
        if (baseCursor.comment == null) {
            if (cursor.comment == null) {
                return true;
            } else {
                LOG.debug("Base sheet={}&cell={} cell comment is null, but second is not",
                    baseCursor.sheet.getSheetName(),
                    baseCursor.cell.getAddress());
                return false;
            }
        }
        if (cursor.comment == null) {
            LOG.debug("Base sheet={}&cell={} cell comment is not null, but second is null",
                baseCursor.sheet.getSheetName(),
                baseCursor.cell.getAddress());
            return false;
        }

        String author1 = getCommentAuthor(baseCursor.comment);
        String author2 = getCommentAuthor(cursor.comment);
        if (!author1.equals(author2)) {
            LOG.debug("Base sheet={}&cell={} cell comment author='{}', but second cell comment author='{}'",
                baseCursor.sheet.getSheetName(),
                baseCursor.cell.getAddress(),
                author1,
                author2);
            return false;
        }

        RichTextString commentTxt1 = baseCursor.comment.getString();
        RichTextString commentTxt2 = cursor.comment.getString();
        if (commentTxt1.length() != commentTxt2.length()) {
            LOG.debug("Base sheet={}&cell={} cell comment length='{}', but second cell comment length='{}'",
                baseCursor.sheet.getSheetName(),
                baseCursor.cell.getAddress(),
                commentTxt1.length(),
                commentTxt2.length());
            return false;
        }
        if (!commentTxt1.getString().equals(commentTxt2.getString())) {
            LOG.debug("Base sheet={}&cell={} cell comment='{}', but second cell comment='{}'",
                baseCursor.sheet.getSheetName(),
                baseCursor.cell.getAddress(),
                commentTxt1.getString(),
                commentTxt2.getString());
            return false;
        }
        return true;
    }

    /**
     * Get comment author. For now, Apache POI doesn't support Threaded Comments, that was introduced in MS Excel 365,
     * such comments has an identifier instead of author name that matches the following pattern {@code tc={UUID}}, if
     * author name matches this pattern, it means that it's a threaded comment, so returns stubbed author. If don't use
     * a stub, copied comment will never visible in Excel
     * 
     * @param comment comment
     * @return return original author or threaded comment stub
     * 
     * @see <a href="https://bz.apache.org/bugzilla/show_bug.cgi?id=65462">Apache POI Threaded Comment Issue</a>
     */
    public static String getCommentAuthor(Comment comment) {
        var match = THREADED_COMMENT_MARKER.matcher(comment.getAuthor());
        if (match.matches()) {
            return "Mock User";
        }
        return comment.getAuthor();
    }

    /**
     * Equals cell styles
     *
     * @param baseCursor base cursor
     * @param cursor second cursor
     * @return {@code true} if no changes is detected, otherwise {@code false}
     */
    static boolean equalStylesInCell(Cursor baseCursor, Cursor cursor) {
        short dataFormat1 = baseCursor.cellStyle.getDataFormat();
        short dataFormat2 = cursor.cellStyle.getDataFormat();
        if (dataFormat1 != dataFormat2) {
            String dataFormatStr1 = baseCursor.cellStyle.getDataFormatString();
            String dataFormatStr2 = cursor.cellStyle.getDataFormatString();
            // For some reason, dataformat index may not be equal, but String representation is equal
            // In this case, there is no visual difference in MS Excel. So, it may sense to assume,
            // if string representation of dataformat is equal, dataformat index may be ignored
            if (!Objects.equals(dataFormatStr1, dataFormatStr2)) {
                LOG.debug("Base sheet={}&cell={} DataFormat='{}', but second DataFormat='{}'",
                    baseCursor.sheet.getSheetName(),
                    baseCursor.cell.getAddress(),
                    dataFormatStr1,
                    dataFormatStr2);
                return false;
            }
        }

        HorizontalAlignment horizontalAlignment1 = baseCursor.cellStyle.getAlignment();
        HorizontalAlignment horizontalAlignment2 = cursor.cellStyle.getAlignment();
        if (horizontalAlignment1 != horizontalAlignment2) {
            LOG.debug("Base sheet={}&cell={} horizontalAlignment='{}', but second horizontalAlignment='{}'",
                baseCursor.sheet.getSheetName(),
                baseCursor.cell.getAddress(),
                horizontalAlignment1,
                horizontalAlignment2);
            return false;
        }

        VerticalAlignment verticalAlignment1 = baseCursor.cellStyle.getVerticalAlignment();
        VerticalAlignment verticalAlignment2 = cursor.cellStyle.getVerticalAlignment();
        if (verticalAlignment1 != verticalAlignment2) {
            LOG.debug("Base sheet={}&cell={} VerticalAlignment='{}', but second VerticalAlignment='{}'",
                baseCursor.sheet.getSheetName(),
                baseCursor.cell.getAddress(),
                verticalAlignment1,
                verticalAlignment2);
            return false;
        }

        short indent1 = baseCursor.cellStyle.getIndention();
        short indent2 = cursor.cellStyle.getIndention();
        if (indent1 != indent2) {
            LOG.debug("Base sheet={}&cell={} Indention='{}', but second Indention='{}'",
                baseCursor.sheet.getSheetName(),
                baseCursor.cell.getAddress(),
                indent1,
                indent2);
            return false;
        }

        short rotation1 = baseCursor.cellStyle.getRotation();
        short rotation2 = cursor.cellStyle.getRotation();
        if (rotation1 != rotation2) {
            LOG.debug("Base sheet={}&cell={} Rotation='{}', but second Rotation='{}'",
                baseCursor.sheet.getSheetName(),
                baseCursor.cell.getAddress(),
                rotation1,
                rotation2);
            return false;
        }

        boolean wrapText1 = baseCursor.cellStyle.getWrapText();
        boolean wrapText2 = cursor.cellStyle.getWrapText();
        if (wrapText1 != wrapText2) {
            LOG.debug("Base sheet={}&cell={} WrapText='{}', but second WrapText='{}'",
                baseCursor.sheet.getSheetName(),
                baseCursor.cell.getAddress(),
                wrapText1,
                wrapText2);
            return false;
        }

        boolean shrinkToFit1 = baseCursor.cellStyle.getShrinkToFit();
        boolean shrinkToFit2 = cursor.cellStyle.getShrinkToFit();
        if (shrinkToFit1 != shrinkToFit2) {
            LOG.debug("Base sheet={}&cell={} ShrinkToFit='{}', but second ShrinkToFit='{}'",
                baseCursor.sheet.getSheetName(),
                baseCursor.cell.getAddress(),
                shrinkToFit1,
                shrinkToFit2);
            return false;
        }

        return equalFontInCell(baseCursor, cursor) && equalBorderInCell(baseCursor,
            cursor) && equalFillInCell(baseCursor, cursor);
    }

    /**
     * Equals cell fonts
     *
     * @param baseCursor base cursor
     * @param cursor second cursor
     * @return {@code true} if no changes is detected, otherwise {@code false}
     */
    private static boolean equalFontInCell(Cursor baseCursor, Cursor cursor) {
        Font baseFont = baseCursor.workbook.getFontAt(baseCursor.cellStyle.getFontIndex());
        Font font = cursor.workbook.getFontAt(cursor.cellStyle.getFontIndex());

        String fontName1 = baseFont.getFontName();
        String fontName2 = font.getFontName();
        if (!fontName1.equals(fontName2)) {
            LOG.debug("Base sheet={}&cell={} FontName='{}', but second FontName='{}'",
                baseCursor.sheet.getSheetName(),
                baseCursor.cell.getAddress(),
                fontName1,
                fontName2);
            return false;
        }

        short fontHeight1 = baseFont.getFontHeightInPoints();
        short fontHeight2 = font.getFontHeightInPoints();
        if (fontHeight1 != fontHeight2) {
            LOG.debug("Base sheet={}&cell={} FontHeightInPoints='{}', but second FontHeightInPoints='{}'",
                baseCursor.sheet.getSheetName(),
                baseCursor.cell.getAddress(),
                fontHeight1,
                fontHeight2);
            return false;
        }

        if (baseFont instanceof XSSFFont && font instanceof XSSFFont) {
            XSSFColor color1 = ((XSSFFont) baseFont).getXSSFColor();
            XSSFColor color2 = ((XSSFFont) font).getXSSFColor();
            if (!equalColor(color1, color2)) {
                LOG.debug("Base sheet={}&cell={} XSSFColor='{}', but second XSSFColor='{}'",
                    baseCursor.sheet.getSheetName(),
                    baseCursor.cell.getAddress(),
                    toARGBHex(color1),
                    toARGBHex(color2));
                return false;
            }
        } else if (baseFont instanceof HSSFFont && font instanceof HSSFFont) {
            HSSFColor color1 = ((HSSFFont) baseFont).getHSSFColor((HSSFWorkbook) baseCursor.originalWorkbook());
            HSSFColor color2 = ((HSSFFont) font).getHSSFColor((HSSFWorkbook) cursor.originalWorkbook());
            if (!equalColor(color1, color2)) {
                LOG.debug("Base sheet={}&cell={} HSSFColor='{}', but second HSSFColor='{}'",
                    baseCursor.sheet.getSheetName(),
                    baseCursor.cell.getAddress(),
                    toARGBHex(color1),
                    toARGBHex(color2));
                return false;
            }
        }

        short color1 = baseFont.getColor();
        short color2 = font.getColor();
        if (color1 != color2) {
            LOG.debug("Base sheet={}&cell={} FontColor='{}', but second FontColor='{}'",
                baseCursor.sheet.getSheetName(),
                baseCursor.cell.getAddress(),
                color1,
                color2);
            return false;
        }

        boolean bold1 = baseFont.getBold();
        boolean bold2 = font.getBold();
        if (bold1 != bold2) {
            LOG.debug("Base sheet={}&cell={} font Bold='{}', but second font Bold='{}'",
                baseCursor.sheet.getSheetName(),
                baseCursor.cell.getAddress(),
                bold1,
                bold2);
            return false;
        }

        byte underline1 = baseFont.getUnderline();
        byte underline2 = font.getUnderline();
        if (underline1 != underline2) {
            LOG.debug("Base sheet={}&cell={} font Underline='{}', but second font Underline='{}'",
                baseCursor.sheet.getSheetName(),
                baseCursor.cell.getAddress(),
                underline1,
                underline2);
            return false;
        }

        boolean italic1 = baseFont.getItalic();
        boolean italic2 = font.getItalic();
        if (italic1 != italic2) {
            LOG.debug("Base sheet={}&cell={} font Italic='{}', but second font Italic='{}'",
                baseCursor.sheet.getSheetName(),
                baseCursor.cell.getAddress(),
                italic1,
                italic2);
            return false;
        }
        return true;
    }

    /**
     * Equals cell borders
     *
     * @param baseCursor base cursor
     * @param cursor second cursor
     * @return {@code true} if no changes is detected, otherwise {@code false}
     */
    private static boolean equalBorderInCell(Cursor baseCursor, Cursor cursor) {
        BorderStyle borderBottom1 = baseCursor.cellStyle.getBorderBottom();
        BorderStyle borderBottom2 = cursor.cellStyle.getBorderBottom();
        if (borderBottom1 != borderBottom2) {
            LOG.debug("Base sheet={}&cell={} BorderBottom='{}', but second BorderBottom='{}'",
                baseCursor.sheet.getSheetName(),
                baseCursor.cell.getAddress(),
                borderBottom1,
                borderBottom2);
            return false;
        }

        BorderStyle borderLeft1 = baseCursor.cellStyle.getBorderLeft();
        BorderStyle borderLeft2 = cursor.cellStyle.getBorderLeft();
        if (borderLeft1 != borderLeft2) {
            LOG.debug("Base sheet={}&cell={} BorderLeft='{}', but second BorderLeft='{}'",
                baseCursor.sheet.getSheetName(),
                baseCursor.cell.getAddress(),
                borderLeft1,
                borderLeft2);
            return false;
        }

        BorderStyle borderTop1 = baseCursor.cellStyle.getBorderTop();
        BorderStyle borderTop2 = cursor.cellStyle.getBorderTop();
        if (borderTop1 != borderTop2) {
            LOG.debug("Base sheet={}&cell={} BorderTop='{}', but second BorderTop='{}'",
                baseCursor.sheet.getSheetName(),
                baseCursor.cell.getAddress(),
                borderTop1,
                borderTop2);
            return false;
        }

        BorderStyle borderRight1 = baseCursor.cellStyle.getBorderRight();
        BorderStyle borderRight2 = cursor.cellStyle.getBorderRight();
        if (borderRight1 != borderRight2) {
            LOG.debug("Base sheet={}&cell={} BorderRight='{}', but second BorderRight='{}'",
                baseCursor.sheet.getSheetName(),
                baseCursor.cell.getAddress(),
                borderRight1,
                borderRight2);
            return false;
        }

        if (baseCursor.cellStyle instanceof XSSFCellStyle) {
            XSSFCellStyle baseCellStyle = (XSSFCellStyle) baseCursor.cellStyle;
            XSSFCellStyle cellStyle = (XSSFCellStyle) cursor.cellStyle;

            XSSFColor bottomBorderColor1 = baseCellStyle.getBottomBorderXSSFColor();
            XSSFColor bottomBorderColor2 = cellStyle.getBottomBorderXSSFColor();
            if (!equalColor(bottomBorderColor1, bottomBorderColor2)) {
                LOG.debug("Base sheet={}&cell={} BottomBorderColor='{}', but second BottomBorderColor='{}'",
                    baseCursor.sheet.getSheetName(),
                    baseCursor.cell.getAddress(),
                    toARGBHex(bottomBorderColor1),
                    toARGBHex(bottomBorderColor2));
                return false;
            }

            XSSFColor leftBorderColor1 = baseCellStyle.getLeftBorderXSSFColor();
            XSSFColor leftBorderColor2 = cellStyle.getLeftBorderXSSFColor();
            if (!equalColor(leftBorderColor1, leftBorderColor2)) {
                LOG.debug("Base sheet={}&cell={} LeftBorderColor='{}', but second LeftBorderColor='{}'",
                    baseCursor.sheet.getSheetName(),
                    baseCursor.cell.getAddress(),
                    toARGBHex(leftBorderColor1),
                    toARGBHex(leftBorderColor2));
                return false;
            }

            XSSFColor topBorderColor1 = baseCellStyle.getTopBorderXSSFColor();
            XSSFColor topBorderColor2 = cellStyle.getTopBorderXSSFColor();
            if (!equalColor(topBorderColor1, topBorderColor2)) {
                LOG.debug("Base sheet={}&cell={} TopBorderColor='{}', but second TopBorderColor='{}'",
                    baseCursor.sheet.getSheetName(),
                    baseCursor.cell.getAddress(),
                    toARGBHex(topBorderColor1),
                    toARGBHex(topBorderColor2));
                return false;
            }

            XSSFColor rightBorderColor1 = baseCellStyle.getRightBorderXSSFColor();
            XSSFColor rightBorderColor2 = cellStyle.getRightBorderXSSFColor();
            if (!equalColor(rightBorderColor1, rightBorderColor2)) {
                LOG.debug("Base sheet={}&cell={} RightBorderColor='{}', but second RightBorderColor='{}'",
                    baseCursor.sheet.getSheetName(),
                    baseCursor.cell.getAddress(),
                    toARGBHex(rightBorderColor1),
                    toARGBHex(rightBorderColor2));
                return false;
            }
        } else {
            short bottomBorderColor1 = baseCursor.cellStyle.getBottomBorderColor();
            short bottomBorderColor2 = cursor.cellStyle.getBottomBorderColor();
            if (bottomBorderColor1 != bottomBorderColor2) {
                LOG.debug("Base sheet={}&cell={} BottomBorderColor='{}', but second BottomBorderColor='{}'",
                    baseCursor.sheet.getSheetName(),
                    baseCursor.cell.getAddress(),
                    bottomBorderColor1,
                    bottomBorderColor2);
                return false;
            }

            short leftBorderColor1 = baseCursor.cellStyle.getLeftBorderColor();
            short leftBorderColor2 = cursor.cellStyle.getLeftBorderColor();
            if (leftBorderColor1 != leftBorderColor2) {
                LOG.debug("Base sheet={}&cell={} LeftBorderColor='{}', but second LeftBorderColor='{}'",
                    baseCursor.sheet.getSheetName(),
                    baseCursor.cell.getAddress(),
                    leftBorderColor1,
                    leftBorderColor2);
                return false;
            }

            short topBorderColor1 = baseCursor.cellStyle.getTopBorderColor();
            short topBorderColor2 = cursor.cellStyle.getTopBorderColor();
            if (topBorderColor1 != topBorderColor2) {
                LOG.debug("Base sheet={}&cell={} TopBorderColor='{}', but second TopBorderColor='{}'",
                    baseCursor.sheet.getSheetName(),
                    baseCursor.cell.getAddress(),
                    topBorderColor1,
                    topBorderColor2);
                return false;
            }

            short rightBorderColor1 = baseCursor.cellStyle.getRightBorderColor();
            short rightBorderColor2 = cursor.cellStyle.getRightBorderColor();
            if (rightBorderColor1 != rightBorderColor2) {
                LOG.debug("Base sheet={}&cell={} RightBorderColor='{}', but second RightBorderColor='{}'",
                    baseCursor.sheet.getSheetName(),
                    baseCursor.cell.getAddress(),
                    rightBorderColor1,
                    rightBorderColor2);
                return false;
            }
        }
        return true;
    }

    /**
     * Equals cell background
     *
     * @param baseCursor base cursor
     * @param cursor second cursor
     * @return {@code true} if no changes is detected, otherwise {@code false}
     */
    private static boolean equalFillInCell(Cursor baseCursor, Cursor cursor) {
        if (baseCursor.cellStyle instanceof XSSFCellStyle) {
            XSSFCellStyle baseCellStyle = (XSSFCellStyle) baseCursor.cellStyle;
            XSSFCellStyle cellStyle = (XSSFCellStyle) cursor.cellStyle;

            XSSFColor fillBackgroundColor1 = baseCellStyle.getFillBackgroundXSSFColor();
            XSSFColor fillBackgroundColor2 = cellStyle.getFillBackgroundXSSFColor();
            if (!equalColor(fillBackgroundColor1, fillBackgroundColor2)) {
                LOG.debug("Base sheet={}&cell={} FillBackgroundColor='{}', but second FillBackgroundColor='{}'",
                    baseCursor.sheet.getSheetName(),
                    baseCursor.cell.getAddress(),
                    toARGBHex(fillBackgroundColor1),
                    toARGBHex(fillBackgroundColor2));
                return false;
            }

            XSSFColor fillForegroundColor1 = baseCellStyle.getFillForegroundXSSFColor();
            XSSFColor fillForegroundColor2 = cellStyle.getFillForegroundXSSFColor();
            if (!equalColor(fillForegroundColor1, fillForegroundColor2)) {
                LOG.debug("Base sheet={}&cell={} FillForegroundColor='{}', but second FillForegroundColor='{}'",
                    baseCursor.sheet.getSheetName(),
                    baseCursor.cell.getAddress(),
                    toARGBHex(fillForegroundColor1),
                    toARGBHex(fillForegroundColor2));
                return false;
            }
        } else {
            short fillBackgroundColor1 = baseCursor.cellStyle.getFillBackgroundColor();
            short fillBackgroundColor2 = cursor.cellStyle.getFillBackgroundColor();
            if (fillBackgroundColor1 != fillBackgroundColor2) {
                LOG.debug("Base sheet={}&cell={} FillBackgroundColor='{}', but second FillBackgroundColor='{}'",
                    baseCursor.sheet.getSheetName(),
                    baseCursor.cell.getAddress(),
                    fillBackgroundColor1,
                    fillBackgroundColor2);
                return false;
            }

            short fillForegroundColor1 = baseCursor.cellStyle.getFillForegroundColor();
            short fillForegroundColor2 = cursor.cellStyle.getFillForegroundColor();
            if (fillForegroundColor1 != fillForegroundColor2) {
                LOG.debug("Base sheet={}&cell={} FillForegroundColor='{}', but second FillForegroundColor='{}'",
                    baseCursor.sheet.getSheetName(),
                    baseCursor.cell.getAddress(),
                    fillForegroundColor1,
                    fillForegroundColor2);
                return false;
            }
        }

        FillPatternType fillPattern1 = baseCursor.cellStyle.getFillPattern();
        FillPatternType fillPattern2 = cursor.cellStyle.getFillPattern();
        if (fillPattern1 != fillPattern2) {
            LOG.debug("Base sheet={}&cell={} FillPattern='{}', but second FillPattern='{}'",
                baseCursor.sheet.getSheetName(),
                baseCursor.cell.getAddress(),
                fillPattern1,
                fillPattern2);
            return false;
        }

        Color fillForegroundColorColor1 = baseCursor.cellStyle.getFillForegroundColorColor();
        Color fillForegroundColorColor2 = cursor.cellStyle.getFillForegroundColorColor();
        if (!Objects.equals(fillForegroundColorColor1, fillForegroundColorColor2)) {
            LOG.debug("Base sheet={}&cell={} FillForegroundColorColor='{}', but second FillForegroundColorColor='{}'",
                baseCursor.sheet.getSheetName(),
                baseCursor.cell.getAddress(),
                fillForegroundColorColor1,
                fillForegroundColorColor2);
            return false;
        }

        Color fillBackgroundColorColor1 = baseCursor.cellStyle.getFillBackgroundColorColor();
        Color fillBackgroundColorColor2 = cursor.cellStyle.getFillBackgroundColorColor();
        if (!Objects.equals(fillBackgroundColorColor1, fillBackgroundColorColor2)) {
            LOG.debug("Base sheet={}&cell={} FillBackgroundColorColor='{}', but second FillBackgroundColorColor='{}'",
                baseCursor.sheet.getSheetName(),
                baseCursor.cell.getAddress(),
                fillBackgroundColorColor1,
                fillBackgroundColorColor2);
            return false;
        }

        return true;
    }

    /**
     * Try to determine, if cell is empty that means that it has not customization
     *
     * @param cell cell
     * @return empty or not
     */
    private static boolean isNullOrEmpty(Cell cell) {
        if (cell == null) {
            return true;
        }
        if (cell.getCellType() != CellType.BLANK) {
            return false;
        }
        if (cell.getCellComment() != null) {
            return false;
        }
        CellStyle cellStyle = cell.getCellStyle();
        if (!BuiltinFormats.getBuiltinFormat(0).equals(cellStyle.getDataFormatString())) {
            return false;
        }
        if (cellStyle.getAlignment() != HorizontalAlignment.GENERAL) {
            return false;
        }
        if (cellStyle.getVerticalAlignment() != VerticalAlignment.BOTTOM) {
            return false;
        }
        if (cellStyle.getIndention() != 0) {
            return false;
        }
        if (cellStyle.getRotation() != 0) {
            return false;
        }
        if (cellStyle.getBorderBottom() != BorderStyle.NONE) {
            return false;
        }
        if (cellStyle.getBorderLeft() != BorderStyle.NONE) {
            return false;
        }
        if (cellStyle.getBorderTop() != BorderStyle.NONE) {
            return false;
        }
        if (cellStyle.getBorderRight() != BorderStyle.NONE) {
            return false;
        }
        if (cellStyle instanceof XSSFCellStyle) {
            XSSFCellStyle xssfCellStyle = (XSSFCellStyle) cellStyle;
            if (!isNullOrEmpty(xssfCellStyle.getBottomBorderXSSFColor())) {
                return false;
            }
            if (!isNullOrEmpty(xssfCellStyle.getLeftBorderXSSFColor())) {
                return false;
            }
            if (!isNullOrEmpty(xssfCellStyle.getTopBorderXSSFColor())) {
                return false;
            }
            if (!isNullOrEmpty(xssfCellStyle.getRightBorderXSSFColor())) {
                return false;
            }
            if (!isNullOrEmpty(xssfCellStyle.getFillBackgroundXSSFColor())) {
                return false;
            }
            if (!isNullOrEmpty(xssfCellStyle.getFillForegroundXSSFColor())) {
                return false;
            }
        } else {
            if (cellStyle.getBottomBorderColor() != 0) {
                return false;
            }
            if (cellStyle.getLeftBorderColor() != 0) {
                return false;
            }
            if (cellStyle.getTopBorderColor() != 0) {
                return false;
            }
            if (cellStyle.getRightBorderColor() != 0) {
                return false;
            }
            if (cellStyle.getFillBackgroundColor() != 0) {
                return false;
            }
            if (cellStyle.getFillForegroundColor() != 0) {
                return false;
            }
        }
        if (cellStyle.getFillPattern() != FillPatternType.NO_FILL) {
            return false;
        }
        for (CellRangeAddress range : cell.getSheet().getMergedRegions()) {
            if (range.isInRange(cell)) {
                return false;
            }
        }
        return true;
    }

    private static boolean equalColor(XSSFColor color1, XSSFColor color2) {
        if (isNullOrEmpty(color1)) {
            return isNullOrEmpty(color2);
        }
        if (isNullOrEmpty(color2)) {
            return false;
        }
        return Objects.equals(color1, color2);
    }

    private static boolean isNullOrEmpty(XSSFColor color) {
        return color == null || (color.getARGB() == null && color.getTheme() == 0 && color.getIndexed() == 0 && color
            .getTint() == 0.0d);
    }

    private static boolean equalColor(HSSFColor color1, HSSFColor color2) {
        if (isNullOrEmpty(color1)) {
            return isNullOrEmpty(color2);
        }
        if (isNullOrEmpty(color2)) {
            return false;
        }
        return Objects.equals(color1, color2);
    }

    private static boolean isNullOrEmpty(HSSFColor color) {
        return color == null || color.getTriplet() == null;
    }

    private static String toARGBHex(XSSFColor color) {
        return color == null ? null : color.getARGBHex();
    }

    private static String toARGBHex(HSSFColor color) {
        return color == null ? null : color.getHexString();
    }

}
