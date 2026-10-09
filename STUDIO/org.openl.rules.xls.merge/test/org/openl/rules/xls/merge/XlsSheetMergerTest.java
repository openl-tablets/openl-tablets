package org.openl.rules.xls.merge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.FormulaError;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFPicture;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTColor;

import org.openl.rules.xls.merge.diff.DiffStatus;
import org.openl.rules.xls.merge.diff.WorkbookDiffResult;

class XlsSheetMergerTest {

    private static final String RULES = "Rules";
    private static final Consumer<Sheet> NO_EDIT = sheet -> {
    };
    private static final byte[] PNG = Base64.getDecoder()
            .decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==");

    /**
     * A rules table and a data table on one sheet, and a second sheet left alone.
     */
    private static void base(Workbook workbook) {
        var sheet = workbook.createSheet(RULES);
        set(sheet, 0, 0, "Rules String rate(String country)");
        sheet.addMergedRegion(CellRangeAddress.valueOf("A1:B1"));
        set(sheet, 1, 0, "C1");
        set(sheet, 1, 1, "RET1");
        set(sheet, 2, 0, "country");
        set(sheet, 2, 1, "rate");
        set(sheet, 3, 0, "US");
        set(sheet, 3, 1, 1);
        set(sheet, 4, 0, "US");
        set(sheet, 4, 1, 2);
        set(sheet, 5, 0, "CA");
        set(sheet, 5, 1, 3);
        cellOf(sheet, 3, 4).setCellFormula("B5*2");
        cellOf(sheet, 3, 4).setCellValue(4);
        set(sheet, 7, 0, "Data Driver drivers");
        set(sheet, 8, 0, "name");
        set(sheet, 8, 1, "age");
        set(sheet, 8, 2, "note");
        set(sheet, 9, 0, "Ann");
        set(sheet, 9, 1, 30);
        set(sheet, 9, 2, "a");
        set(sheet, 10, 0, "Bob");
        set(sheet, 10, 1, 40);
        set(sheet, 10, 2, "b");
        set(sheet, 9, 4, "reviewed");
        var filled = workbook.createCellStyle();
        filled.setFillForegroundColor(IndexedColors.YELLOW.getIndex());
        filled.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        sheet.getRow(9).getCell(4).setCellStyle(filled);
        comment(sheet, 9, 4, "by John");
        var formatting = sheet.getSheetConditionalFormatting();
        var rule = formatting.createConditionalFormattingRule("B4>1");
        var fill = rule.createPatternFormatting();
        if (workbook instanceof XSSFWorkbook) {
            fill.setFillBackgroundColor(themed(4));
        } else {
            fill.setFillBackgroundColor(IndexedColors.YELLOW.getIndex());
        }
        rule.createFontFormatting().setFontStyle(false, true);
        rule.createBorderFormatting().setBorderBottom(BorderStyle.THIN);
        formatting.addConditionalFormatting(new CellRangeAddress[]{CellRangeAddress.valueOf("B4:B6")}, rule);
        var other = workbook.createSheet("Other");
        set(other, 0, 0, "Other");
        cellOf(other, 0, 1).setCellFormula("Rules!B5*2");
        cellOf(other, 0, 1).setCellValue(4);
    }

    @Test
    void differentTablesOfOneSheet_areMerged() throws IOException {
        var merged = mergeXlsx(sheet -> set(sheet, 3, 1, 10), sheet -> set(sheet, 9, 1, 31));
        assertEquals(10, number(merged, 3, 1));
        assertEquals(31, number(merged, 9, 1));
    }

    @Test
    void differentRowsOfOneTable_areMerged() throws IOException {
        var merged = mergeXlsx(sheet -> set(sheet, 3, 1, 10), sheet -> set(sheet, 5, 1, 30));
        assertEquals(10, number(merged, 3, 1));
        assertEquals(30, number(merged, 5, 1));
        assertEquals(2, number(merged, 4, 1));
    }

    @Test
    void rowsAppendedAfterTable_areMerged() throws IOException {
        var merged = mergeXlsx(sheet -> set(sheet, 3, 1, 10), sheet -> {
            set(sheet, 11, 0, "Cid");
            set(sheet, 11, 1, 50);
            set(sheet, 11, 2, "c");
        });
        assertEquals(10, number(merged, 3, 1));
        assertEquals("Cid", text(merged, 11, 0));
        assertEquals(50, number(merged, 11, 1));
    }

    @Test
    void sameCellChangedTheSameWay_isNotConflict() throws IOException {
        var merged = mergeXlsx(sheet -> {
            set(sheet, 3, 1, 10);
            set(sheet, 4, 1, 20);
        }, sheet -> set(sheet, 3, 1, 10));
        assertEquals(10, number(merged, 3, 1));
        assertEquals(20, number(merged, 4, 1));
    }

    @Test
    void valueInOursAndStyleInTheirsOfOneCell_areMerged() throws IOException {
        var merged = mergeXlsx(sheet -> set(sheet, 3, 1, 10), sheet -> bold(sheet, 3, 1));
        assertEquals(10, number(merged, 3, 1));
        assertTrue(isBold(merged, 3, 1));
    }

    @Test
    void styleInOursAndValueInTheirsOfOneCell_areMerged() throws IOException {
        var merged = mergeXlsx(sheet -> bold(sheet, 3, 1), sheet -> set(sheet, 3, 1, 10));
        assertEquals(10, number(merged, 3, 1));
        assertTrue(isBold(merged, 3, 1));
    }

    @Test
    void valueClearedInTheirsAndStyleInOursOfOneCell_areMerged() throws IOException {
        var merged = mergeXlsx(sheet -> {
            var style = sheet.getWorkbook().createCellStyle();
            style.setFillForegroundColor(IndexedColors.YELLOW.getIndex());
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            sheet.getRow(4).getCell(1).setCellStyle(style);
        }, sheet -> sheet.getRow(4).getCell(1).setBlank());
        var cleared = cell(merged, 4, 1);
        assertEquals(CellType.BLANK, cleared.getCellType());
        assertEquals(FillPatternType.SOLID_FOREGROUND, cleared.getCellStyle().getFillPattern());
    }

    @Test
    void commentInTheirsAndValueInOursOfOneCell_areMerged() throws IOException {
        var merged = mergeXlsx(sheet -> set(sheet, 3, 0, "UK"), sheet -> comment(sheet, 3, 0, "checked"));
        assertEquals("UK", text(merged, 3, 0));
        assertEquals("checked", cell(merged, 3, 0).getCellComment().getString().getString());
    }

    @Test
    void styleOfOneCellChangedDifferently_isConflict() throws IOException {
        assertConflict(sheet -> bold(sheet, 3, 1), sheet -> {
            var workbook = sheet.getWorkbook();
            var style = workbook.createCellStyle();
            style.setAlignment(HorizontalAlignment.CENTER);
            sheet.getRow(3).getCell(1).setCellStyle(style);
        });
    }

    @Test
    void sameCellChangedDifferently_isConflict() throws IOException {
        assertConflict(sheet -> set(sheet, 3, 1, 10), sheet -> set(sheet, 3, 1, 11));
    }

    @Test
    void rowInsertedInsideTable_isConflict() throws IOException {
        // Without the check the edit of A5 would land on the inserted row: OUR A5 holds the same "US" as BASE A5.
        assertConflict(sheet -> {
            sheet.shiftRows(3, sheet.getLastRowNum(), 1);
            set(sheet, 3, 0, "UK");
            set(sheet, 3, 1, 9);
        }, sheet -> set(sheet, 4, 0, "MX"));
    }

    @Test
    void rowDeletedInsideTable_isConflict() throws IOException {
        assertConflict(sheet -> set(sheet, 9, 1, 31), sheet -> {
            sheet.removeRow(sheet.getRow(4));
            sheet.shiftRows(5, sheet.getLastRowNum(), -1);
        });
    }

    @Test
    void columnDeleted_isConflict() throws IOException {
        assertConflict(sheet -> set(sheet, 3, 1, 10), sheet -> {
            for (var row : sheet) {
                var cell = row.getCell(1);
                if (cell != null) {
                    row.removeCell(cell);
                }
            }
            sheet.shiftColumns(2, 2, -1);
        });
    }

    @Test
    void styleChangedInTheirs_isMerged() throws IOException {
        var merged = mergeXlsx(sheet -> set(sheet, 3, 1, 10), sheet -> {
            var workbook = sheet.getWorkbook();
            var font = workbook.createFont();
            font.setBold(true);
            var style = workbook.createCellStyle();
            style.setFont(font);
            sheet.getRow(9).getCell(0).setCellStyle(style);
        });
        assertEquals(10, number(merged, 3, 1));
        var style = cell(merged, 9, 0).getCellStyle();
        assertTrue(merged.getWorkbook().getFontAt(style.getFontIndex()).getBold());
    }

    @Test
    void cellClearedInTheirs_isMerged() throws IOException {
        var merged = mergeXlsx(sheet -> set(sheet, 3, 1, 10), sheet -> {
            var row = sheet.getRow(10);
            row.removeCell(row.getCell(2));
        });
        assertEquals(10, number(merged, 3, 1));
        assertNull(cell(merged, 10, 2));
    }

    @Test
    void cellWithStyleAndCommentRemovedInTheirs_isMerged() throws IOException {
        var merged = mergeXlsx(sheet -> set(sheet, 3, 1, 10), sheet -> {
            var row = sheet.getRow(9);
            row.getCell(4).removeCellComment();
            row.removeCell(row.getCell(4));
        });
        assertNull(cell(merged, 9, 4));
        assertNull(merged.getCellComment(new CellAddress(9, 4)));
        assertEquals(10, number(merged, 3, 1));
    }

    @Test
    void commentAddedInTheirs_isMerged() throws IOException {
        var merged = mergeXlsx(sheet -> set(sheet, 3, 1, 10), sheet -> {
            var workbook = sheet.getWorkbook();
            var anchor = workbook.getCreationHelper().createClientAnchor();
            anchor.setRow1(3);
            anchor.setRow2(5);
            anchor.setCol1(0);
            anchor.setCol2(2);
            var comment = sheet.createDrawingPatriarch().createCellComment(anchor);
            comment.setString(workbook.getCreationHelper().createRichTextString("checked"));
            comment.setAuthor("John");
            sheet.getRow(3).getCell(0).setCellComment(comment);
        });
        assertEquals(10, number(merged, 3, 1));
        assertEquals("checked", cell(merged, 3, 0).getCellComment().getString().getString());
    }

    @Test
    void formulaAddedInTheirs_isMergedAndEvaluated() throws IOException {
        var merged = mergeXlsx(sheet -> set(sheet, 4, 1, 20), sheet -> sheet.getRow(3).createCell(2).setCellFormula("B4*2"));
        var formula = cell(merged, 3, 2);
        assertEquals(CellType.FORMULA, formula.getCellType());
        assertEquals("B4*2", formula.getCellFormula());
        assertEquals(2, formula.getNumericCellValue());
        assertEquals(20, number(merged, 4, 1));
    }

    @Test
    void mergedRegionAddedInTheirs_isMerged() throws IOException {
        var merged = mergeXlsx(sheet -> set(sheet, 9, 1, 31),
                sheet -> sheet.addMergedRegion(CellRangeAddress.valueOf("A8:B8")));
        assertEquals(List.of(CellRangeAddress.valueOf("A1:B1"), CellRangeAddress.valueOf("A8:B8")),
                merged.getMergedRegions());
        assertEquals(31, number(merged, 9, 1));
    }

    @Test
    void valuesOfEveryKind_areMerged() throws IOException {
        var merged = mergeXlsx(sheet -> set(sheet, 3, 1, 10), sheet -> {
            sheet.getRow(3).createCell(3).setCellValue(true);
            sheet.getRow(4).createCell(3).setCellErrorValue(FormulaError.DIV0.getCode());
            sheet.getRow(5).createCell(3);
            sheet.removeMergedRegion(0);
        });
        assertTrue(cell(merged, 3, 3).getBooleanCellValue());
        assertEquals(FormulaError.DIV0.getCode(), cell(merged, 4, 3).getErrorCellValue());
        assertTrue(merged.getMergedRegions().isEmpty());
        assertEquals(10, number(merged, 3, 1));
    }

    @Test
    void formulaReplacedByConstantInTheirs_isMerged() throws IOException {
        var merged = mergeXlsx(sheet -> set(sheet, 3, 1, 10), sheet -> {
            var row = sheet.getRow(3);
            row.removeCell(row.getCell(4));
            set(sheet, 3, 4, 7);
        });
        assertEquals(CellType.NUMERIC, cell(merged, 3, 4).getCellType());
        assertEquals(7, number(merged, 3, 4));
    }

    @Test
    void formulaOfOursDependingOnTheirCell_isRecalculated() throws IOException {
        var merged = mergeXlsx(sheet -> set(sheet, 3, 1, 10), sheet -> set(sheet, 4, 1, 20));
        assertEquals("B5*2", cell(merged, 3, 4).getCellFormula());
        assertEquals(40, cell(merged, 3, 4).getNumericCellValue());
    }

    @Test
    void formulaThatCannotBeCalculated_keepsItsValue() throws IOException {
        var merged = mergeXlsx(sheet -> {
            set(sheet, 3, 1, 10);
            cellOf(sheet, 4, 4).setCellFormula("CUBEVALUE(\"cube\")");
            cellOf(sheet, 4, 4).setCellValue("kept");
        }, sheet -> set(sheet, 9, 1, 31));
        assertEquals("CUBEVALUE(\"cube\")", cell(merged, 4, 4).getCellFormula());
        assertEquals("kept", cell(merged, 4, 4).getStringCellValue());
        assertEquals(31, number(merged, 9, 1));
    }

    @Test
    void inlineStringOfOurs_isReplaced() throws IOException {
        // SXSSF writes text as inline strings, which keep their text when only a value is set into them.
        var merged = merge(XSSFWorkbook::new, SXSSFWorkbook::new, sheet -> set(sheet, 3, 1, 10),
                sheet -> set(sheet, 4, 0, "MX"));
        assertEquals("MX", text(merged, 4, 0));
        assertEquals(10, number(merged, 3, 1));
    }

    @Test
    void rowHeightsFittedToContent_areNotConflict() throws IOException {
        var merged = mergeXlsx(sheet -> {
            set(sheet, 3, 1, 10);
            ((XSSFRow) sheet.getRow(9)).getCTRow().setHt(30);
        }, sheet -> {
            set(sheet, 9, 1, 31);
            ((XSSFRow) sheet.getRow(9)).getCTRow().setHt(40);
        });
        assertEquals(10, number(merged, 3, 1));
        assertEquals(31, number(merged, 9, 1));
    }

    @Test
    void widthOfEmptyColumnChangedInTheirs_isMerged() throws IOException {
        var merged = mergeXlsx(sheet -> set(sheet, 3, 1, 10), sheet -> {
            set(sheet, 9, 1, 31);
            sheet.setColumnWidth(7, 9000);
        });
        assertEquals(9000, merged.getColumnWidth(7));
    }

    @Test
    void hyperlinkAddedInTheirs_isConflict() throws IOException {
        assertConflict(sheet -> set(sheet, 3, 1, 10), sheet -> {
            set(sheet, 9, 1, 31);
            var link = sheet.getWorkbook().getCreationHelper().createHyperlink(HyperlinkType.URL);
            link.setAddress("https://openl-tablets.org");
            sheet.getRow(9).getCell(0).setHyperlink(link);
        });
    }

    @Test
    void paneFrozenInTheirs_isConflict() throws IOException {
        assertConflict(sheet -> set(sheet, 3, 1, 10), sheet -> {
            set(sheet, 9, 1, 31);
            sheet.createFreezePane(0, 1);
        });
    }

    @Test
    void conditionalFormattingStyleChangedInTheirs_isConflict() throws IOException {
        assertConflict(sheet -> set(sheet, 3, 1, 10), sheet -> {
            set(sheet, 9, 1, 31);
            var rule = sheet.getSheetConditionalFormatting().getConditionalFormattingAt(0).getRule(0);
            rule.getPatternFormatting().setFillBackgroundColor(IndexedColors.RED.getIndex());
        });
    }

    @Test
    void newCellOfTheirsWithFont_keepsItsStyle() throws IOException {
        var merged = mergeXlsx(sheet -> set(sheet, 3, 1, 10), sheet -> {
            set(sheet, 1, 6, "Total");
            bold(sheet, 1, 6);
        });
        assertEquals("Total", text(merged, 1, 6));
        assertTrue(isBold(merged, 1, 6));
    }

    @Test
    void formulaOfTheirsThatCannotBeCalculated_isMerged() throws IOException {
        var merged = mergeXlsx(sheet -> set(sheet, 3, 1, 10),
                sheet -> cellOf(sheet, 1, 6).setCellFormula("CUBEVALUE(\"cube\")"));
        assertEquals("CUBEVALUE(\"cube\")", cell(merged, 1, 6).getCellFormula());
        assertEquals(10, number(merged, 3, 1));
    }

    @Test
    void formulaOfAnotherSheet_isRecalculated() throws IOException {
        var merged = mergeXlsx(sheet -> set(sheet, 3, 1, 10), sheet -> set(sheet, 4, 1, 20));
        var other = merged.getWorkbook().getSheet("Other");
        assertEquals(40, other.getRow(0).getCell(1).getNumericCellValue());
    }

    @Test
    void cellsFilledApart_areMerged() throws IOException {
        var merged = mergeXlsx(sheet -> set(sheet, 13, 0, "Data Car cars"), sheet -> set(sheet, 16, 0, "Data Bus buses"));
        assertEquals("Data Car cars", text(merged, 13, 0));
        assertEquals("Data Bus buses", text(merged, 16, 0));
    }

    @Test
    void cellsFilledNextToEachOther_isConflict() throws IOException {
        // A row filled in each branch, one under the other: OpenL would read them as one table.
        assertConflict(sheet -> {
            set(sheet, 13, 0, "Data Car cars");
            set(sheet, 3, 1, 10);
        }, sheet -> set(sheet, 14, 0, "model"));
    }

    @Test
    void mergedCellsOfTheirsOverValueOfOurs_isConflict() throws IOException {
        assertConflict(sheet -> set(sheet, 10, 1, 41),
                sheet -> sheet.addMergedRegion(CellRangeAddress.valueOf("A11:B11")));
    }

    @Test
    void mergedCellsOfOursOverValueOfTheirs_isConflict() throws IOException {
        assertConflict(sheet -> sheet.addMergedRegion(CellRangeAddress.valueOf("A11:B11")),
                sheet -> set(sheet, 10, 1, 41));
    }

    @Test
    void dateFormatInTheirsAndValueInOurs_areMerged() throws IOException {
        var merged = mergeXlsx(sheet -> set(sheet, 9, 1, 45001), sheet -> {
            var workbook = sheet.getWorkbook();
            var style = workbook.createCellStyle();
            style.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat("yyyy-mm-dd"));
            sheet.getRow(9).getCell(1).setCellStyle(style);
        });
        assertEquals(45001, number(merged, 9, 1));
        assertEquals("yyyy-mm-dd", cell(merged, 9, 1).getCellStyle().getDataFormatString());
    }

    @Test
    void dataValidationPromptChangedInTheirs_isConflict() throws IOException {
        var diff = diff(XSSFWorkbook::new, sheet -> {
            addValidation(sheet);
            set(sheet, 3, 1, 10);
        }, sheet -> {
            addValidation(sheet).createPromptBox("Country", "Pick a country");
            set(sheet, 9, 1, 31);
        });
        assertTrue(diff.hasConflicts());
    }

    @Test
    void conditionalFormattingThemeColorChangedInTheirs_isConflict() throws IOException {
        assertConflict(sheet -> set(sheet, 3, 1, 10), sheet -> {
            set(sheet, 9, 1, 31);
            var rule = sheet.getSheetConditionalFormatting().getConditionalFormattingAt(0).getRule(0);
            rule.getPatternFormatting().setFillBackgroundColor(themed(5));
        });
    }

    @Test
    void mergedRegionsChangedInBoth_isConflict() throws IOException {
        assertConflict(sheet -> sheet.addMergedRegion(CellRangeAddress.valueOf("A8:B8")),
                sheet -> sheet.addMergedRegion(CellRangeAddress.valueOf("A9:B9")));
    }

    @Test
    void columnWidthAndRowHeightChangedInTheirs_areMerged() throws IOException {
        var merged = mergeXlsx(sheet -> set(sheet, 3, 1, 10), sheet -> {
            set(sheet, 9, 1, 31);
            sheet.setColumnWidth(0, 8000);
            sheet.getRow(0).setHeight((short) 600);
        });
        assertEquals(8000, merged.getColumnWidth(0));
        assertEquals(600, merged.getRow(0).getHeight());
        assertEquals(10, number(merged, 3, 1));
    }

    @Test
    void defaultLayoutChangedInTheirs_isMerged() throws IOException {
        var merged = mergeXlsx(sheet -> set(sheet, 3, 1, 10), sheet -> {
            set(sheet, 9, 1, 31);
            sheet.setDefaultColumnWidth(20);
            sheet.setDefaultRowHeight((short) 400);
        });
        assertEquals(20, merged.getDefaultColumnWidth());
        assertEquals(400, merged.getDefaultRowHeight());
        assertEquals(20 * 256, merged.getColumnWidth(30));
        assertEquals(10, number(merged, 3, 1));
    }

    @Test
    void defaultColumnWidthChangedDifferently_isConflict() throws IOException {
        assertConflict(sheet -> {
            set(sheet, 3, 1, 10);
            sheet.setDefaultColumnWidth(20);
        }, sheet -> {
            set(sheet, 9, 1, 31);
            sheet.setDefaultColumnWidth(25);
        });
    }

    @Test
    void columnWidthChangedDifferently_isConflict() throws IOException {
        assertConflict(sheet -> {
            set(sheet, 3, 1, 10);
            sheet.setColumnWidth(0, 8000);
        }, sheet -> {
            set(sheet, 9, 1, 31);
            sheet.setColumnWidth(0, 9000);
        });
    }

    @Test
    void rowHeightChangedDifferently_isConflict() throws IOException {
        assertConflict(sheet -> {
            set(sheet, 3, 1, 10);
            sheet.getRow(0).setHeight((short) 600);
        }, sheet -> {
            set(sheet, 9, 1, 31);
            sheet.getRow(0).setHeight((short) 700);
        });
    }

    @Test
    void sheetHiddenInTheirs_isMerged() throws IOException {
        var merged = mergeXlsx(sheet -> set(sheet, 3, 1, 10), sheet -> {
            set(sheet, 9, 1, 31);
            var workbook = sheet.getWorkbook();
            workbook.setActiveSheet(1);
            workbook.setSheetHidden(workbook.getSheetIndex(sheet), true);
        });
        assertTrue(merged.getWorkbook().isSheetHidden(merged.getWorkbook().getSheetIndex(RULES)));
        assertEquals(10, number(merged, 3, 1));
    }

    @Test
    void pictureAddedInTheirs_isMerged() throws IOException {
        var merged = mergeXlsx(sheet -> set(sheet, 3, 1, 10), XlsSheetMergerTest::addPicture);
        var pictures = ((XSSFDrawing) merged.getDrawingPatriarch()).getShapes()
                .stream()
                .filter(XSSFPicture.class::isInstance)
                .toList();
        assertEquals(1, pictures.size());
        assertEquals(10, number(merged, 3, 1));
    }

    @Test
    void picturesChangedInBoth_isConflict() throws IOException {
        assertConflict(sheet -> {
            addPicture(sheet);
            set(sheet, 3, 1, 10);
        }, sheet -> {
            addPicture(sheet);
            addPicture(sheet);
        });
    }

    @Test
    void dataValidationAddedInTheirs_isConflict() throws IOException {
        assertConflict(sheet -> set(sheet, 3, 1, 10), sheet -> {
            set(sheet, 9, 1, 31);
            var helper = sheet.getDataValidationHelper();
            var constraint = helper.createExplicitListConstraint(new String[]{"US", "CA"});
            sheet.addValidationData(helper.createValidation(constraint, new CellRangeAddressList(3, 5, 0, 0)));
        });
    }

    @Test
    void conditionalFormattingAddedInTheirs_isConflict() throws IOException {
        assertConflict(sheet -> set(sheet, 3, 1, 10), sheet -> {
            set(sheet, 9, 1, 31);
            var formatting = sheet.getSheetConditionalFormatting();
            var rule = formatting.createConditionalFormattingRule("B4>1");
            formatting.addConditionalFormatting(new CellRangeAddress[]{CellRangeAddress.valueOf("B4:B6")}, rule);
        });
    }

    @Test
    void xlsWorkbook_isMerged() throws IOException {
        var merged = merge(HSSFWorkbook::new, sheet -> set(sheet, 3, 1, 10), sheet -> set(sheet, 9, 1, 31));
        assertEquals(10, number(merged, 3, 1));
        assertEquals(31, number(merged, 9, 1));
    }

    @Test
    void mergedSheet_isListedAsMerged() throws IOException {
        var diff = diff(XSSFWorkbook::new, sheet -> set(sheet, 3, 1, 10), sheet -> set(sheet, 9, 1, 31));
        var sheets = diff.getSheetDiffResult();
        assertEquals(List.of(RULES), List.copyOf(sheets.getDiffSheets(DiffStatus.MERGED)));
        assertTrue(sheets.getDiffSheets(DiffStatus.CONFLICT).isEmpty());
        assertTrue(diff.hasChangesToMerge());
        assertFalse(diff.hasConflicts());
    }

    private static void assertConflict(Consumer<Sheet> our, Consumer<Sheet> their) throws IOException {
        var diff = diff(XSSFWorkbook::new, our, their);
        assertTrue(diff.hasConflicts());
        assertEquals(List.of(RULES), List.copyOf(diff.getSheetDiffResult().getDiffSheets(DiffStatus.CONFLICT)));
    }

    private static Sheet mergeXlsx(Consumer<Sheet> our, Consumer<Sheet> their) throws IOException {
        return merge(XSSFWorkbook::new, our, their);
    }

    /**
     * Merges OUR and THEIR revisions of the sheet against BASE revision, and returns the merged sheet.
     */
    private static Sheet merge(Supplier<Workbook> format, Consumer<Sheet> our, Consumer<Sheet> their) throws IOException {
        return merge(format, format, our, their);
    }

    /**
     * Merges OUR revision, written in its own format, and THEIR revision of the sheet against BASE revision.
     */
    private static Sheet merge(Supplier<Workbook> format, Supplier<Workbook> ourFormat, Consumer<Sheet> our, Consumer<Sheet> their)
            throws IOException {
        var ourBytes = write(ourFormat, our);
        var theirBytes = write(format, their);
        var diff = diff(write(format, NO_EDIT), ourBytes, theirBytes);
        assertFalse(diff.hasConflicts());
        var output = new ByteArrayOutputStream();
        XlsWorkbookMerger.merge(new ByteArrayInputStream(ourBytes), new ByteArrayInputStream(theirBytes), diff, output);
        return WorkbookFactory.create(new ByteArrayInputStream(output.toByteArray())).getSheet(RULES);
    }

    private static WorkbookDiffResult diff(Supplier<Workbook> format, Consumer<Sheet> our, Consumer<Sheet> their) throws IOException {
        return diff(write(format, NO_EDIT), write(format, our), write(format, their));
    }

    private static WorkbookDiffResult diff(byte[] base, byte[] our, byte[] their) throws IOException {
        try (var merger = XlsWorkbookMerger.create(new ByteArrayInputStream(base),
                new ByteArrayInputStream(our),
                new ByteArrayInputStream(their))) {
            return merger.getDiffResult();
        }
    }

    private static byte[] write(Supplier<Workbook> format, Consumer<Sheet> edit) throws IOException {
        try (var workbook = format.get(); var output = new ByteArrayOutputStream()) {
            base(workbook);
            edit.accept(workbook.getSheet(RULES));
            workbook.write(output);
            return output.toByteArray();
        }
    }

    private static XSSFColor themed(int theme) {
        var color = XSSFColor.from(CTColor.Factory.newInstance(), null);
        color.setTheme(theme);
        return color;
    }

    private static DataValidation addValidation(Sheet sheet) {
        var helper = sheet.getDataValidationHelper();
        var constraint = helper.createExplicitListConstraint(new String[]{"US", "CA"});
        var validation = helper.createValidation(constraint, new CellRangeAddressList(3, 5, 0, 0));
        sheet.addValidationData(validation);
        return validation;
    }

    private static void bold(Sheet sheet, int row, int column) {
        var workbook = sheet.getWorkbook();
        var font = workbook.createFont();
        font.setBold(true);
        var style = workbook.createCellStyle();
        style.setFont(font);
        sheet.getRow(row).getCell(column).setCellStyle(style);
    }

    private static boolean isBold(Sheet sheet, int row, int column) {
        var style = cell(sheet, row, column).getCellStyle();
        return sheet.getWorkbook().getFontAt(style.getFontIndex()).getBold();
    }

    private static void comment(Sheet sheet, int row, int column, String text) {
        var workbook = sheet.getWorkbook();
        var anchor = workbook.getCreationHelper().createClientAnchor();
        anchor.setRow1(row);
        anchor.setRow2(row + 2);
        anchor.setCol1(column);
        anchor.setCol2(column + 2);
        var comment = sheet.createDrawingPatriarch().createCellComment(anchor);
        comment.setString(workbook.getCreationHelper().createRichTextString(text));
        comment.setAuthor("John");
        sheet.getRow(row).getCell(column).setCellComment(comment);
    }

    private static void addPicture(Sheet sheet) {
        var workbook = sheet.getWorkbook();
        var index = workbook.addPicture(PNG, Workbook.PICTURE_TYPE_PNG);
        var anchor = workbook.getCreationHelper().createClientAnchor();
        anchor.setCol1(5);
        anchor.setRow1(1);
        anchor.setCol2(6);
        anchor.setRow2(2);
        sheet.createDrawingPatriarch().createPicture(anchor, index);
    }

    private static void set(Sheet sheet, int row, int column, String value) {
        cellOf(sheet, row, column).setCellValue(value);
    }

    private static void set(Sheet sheet, int row, int column, double value) {
        cellOf(sheet, row, column).setCellValue(value);
    }

    private static Cell cellOf(Sheet sheet, int row, int column) {
        var sheetRow = sheet.getRow(row) == null ? sheet.createRow(row) : sheet.getRow(row);
        return sheetRow.getCell(column) == null ? sheetRow.createCell(column) : sheetRow.getCell(column);
    }

    private static Cell cell(Sheet sheet, int row, int column) {
        var sheetRow = sheet.getRow(row);
        return sheetRow == null ? null : sheetRow.getCell(column);
    }

    private static double number(Sheet sheet, int row, int column) {
        return cell(sheet, row, column).getNumericCellValue();
    }

    private static String text(Sheet sheet, int row, int column) {
        return cell(sheet, row, column).getStringCellValue();
    }
}
