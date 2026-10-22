package org.openl.rules.xls.merge;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.formula.FormulaParseException;
import org.apache.poi.ss.formula.FormulaParser;
import org.apache.poi.ss.formula.FormulaParsingWorkbook;
import org.apache.poi.ss.formula.FormulaType;
import org.apache.poi.ss.formula.SheetIdentifier;
import org.apache.poi.ss.formula.SheetRangeIdentifier;
import org.apache.poi.ss.formula.ptg.Ptg;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.AreaReference;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.ss.util.CellUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Writes a value typed for a cell the way Excel reads what is typed into a cell.
 *
 * <p>A text after the equals sign is a formula. TRUE and FALSE, in any case, are booleans. A number is written with a
 * dot before its fraction, and may have an exponent. A date is written as {@code 2026-10-22}, and a date with a time as
 * {@code 2026-10-22 14:30} or {@code 2026-10-22T14:30:15}; a cell that is not formatted as a date takes a date format
 * then, as Excel gives one. A date before the first day the workbook counts its dates from, 1900-01-01 or 1904-01-01,
 * is text, as Excel keeps it. Anything else is text, and so is a text after an apostrophe, which keeps a number or a
 * formula as text. An empty value clears the cell.
 *
 * <p>The cell keeps its style and its comment.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@NullMarked
final class TypedValues {

    private static final Pattern NUMBER = Pattern.compile("[+-]?(\\d+(\\.\\d*)?|\\.\\d+)([eE][+-]?\\d+)?");
    private static final Pattern DATE_TIME = Pattern.compile("\\d{4}-\\d{2}-\\d{2}[ T]\\d{2}:\\d{2}(:\\d{2})?");
    private static final String DATE_FORMAT = "yyyy-mm-dd";
    private static final String DATE_TIME_FORMAT = "yyyy-mm-dd hh:mm:ss";
    private static final LocalDate FIRST_DATE = LocalDate.of(1900, 1, 1);
    private static final LocalDate FIRST_DATE_1904 = LocalDate.of(1904, 1, 1);

    /**
     * Tells whether a typed value is a formula that the merged workbook cannot read: a sheet the merged workbook has
     * not got, a name or a function it does not know, or broken syntax. A sheet of another workbook is not checked.
     *
     * @param sheet    the sheet of OUR revision the formula is typed into, which knows the names of the workbook
     * @param hasSheet tells whether the merged workbook has a sheet, by its name
     */
    static boolean isUnreadableFormula(String text, Sheet sheet, Predicate<String> hasSheet) {
        if (!isFormula(text)) {
            return false;
        }
        var workbook = sheet.getWorkbook();
        var parsing = XlsFormulaReferences.parsingOf(workbook);
        if (parsing == null) {
            return false;
        }
        try {
            FormulaParser.parse(text.substring(1),
                    new KnownSheets(parsing, hasSheet),
                    FormulaType.CELL,
                    workbook.getSheetIndex(sheet));
            return false;
        } catch (FormulaParseException e) {
            return true;
        }
    }

    /**
     * Writes a typed value into a cell, in place of the value it has.
     */
    static void write(Cell cell, String text) {
        cell.setBlank();
        if (text.isEmpty()) {
            return;
        }
        if (isFormula(text)) {
            cell.setCellFormula(text.substring(1));
        } else if (text.charAt(0) == '\'') {
            cell.setCellValue(text.substring(1));
        } else if (isBoolean(text)) {
            cell.setCellValue(Boolean.parseBoolean(text));
        } else if (NUMBER.matcher(text).matches()) {
            cell.setCellValue(Double.parseDouble(text));
        } else {
            writeText(cell, text);
        }
    }

    private static boolean isFormula(String text) {
        return text.length() > 1 && text.charAt(0) == '=';
    }

    private static boolean isBoolean(String text) {
        var lower = text.toLowerCase(Locale.ROOT);
        return "true".equals(lower) || "false".equals(lower);
    }

    /**
     * Writes a date, a date with a time, or else the text itself.
     */
    private static void writeText(Cell cell, String text) {
        var first = isDate1904(cell.getSheet().getWorkbook()) ? FIRST_DATE_1904 : FIRST_DATE;
        var date = dateOf(text);
        if (date != null && !date.isBefore(first)) {
            cell.setCellValue(date);
            formatAsDate(cell, DATE_FORMAT);
            return;
        }
        var dateTime = dateTimeOf(text);
        if (dateTime != null && !dateTime.toLocalDate().isBefore(first)) {
            cell.setCellValue(dateTime);
            formatAsDate(cell, DATE_TIME_FORMAT);
            return;
        }
        cell.setCellValue(text);
    }

    private static @Nullable LocalDate dateOf(String text) {
        try {
            return text.length() == DATE_FORMAT.length() ? LocalDate.parse(text) : null;
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static @Nullable LocalDateTime dateTimeOf(String text) {
        if (!DATE_TIME.matcher(text).matches()) {
            return null;
        }
        try {
            return LocalDateTime.parse(text.replace(' ', 'T'));
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static boolean isDate1904(Workbook workbook) {
        return switch (workbook) {
            case XSSFWorkbook xssf -> xssf.isDate1904();
            case HSSFWorkbook hssf -> hssf.getInternalWorkbook().isUsing1904DateWindowing();
            default -> false;
        };
    }

    /**
     * Gives a date format to a cell that shows its date as a number.
     */
    private static void formatAsDate(Cell cell, String format) {
        if (!DateUtil.isCellDateFormatted(cell)) {
            var dataFormat = cell.getSheet().getWorkbook().createDataFormat().getFormat(format);
            CellUtil.setCellStyleProperty(cell, CellUtil.DATA_FORMAT, dataFormat);
        }
    }

    /**
     * The workbook a typed formula is read against, which knows the sheets of the merged workbook. A sheet of the
     * workbook itself that the merged workbook has not got cannot be read.
     */
    @RequiredArgsConstructor
    private static final class KnownSheets implements FormulaParsingWorkbook {

        @Delegate(excludes = SheetReferences.class)
        private final FormulaParsingWorkbook workbook;
        private final Predicate<String> hasSheet;

        @Override
        public Ptg getNameXPtg(String name, @Nullable SheetIdentifier sheet) {
            requireKnown(sheet);
            return workbook.getNameXPtg(name, sheet);
        }

        @Override
        public Ptg get3DReferencePtg(CellReference cell, SheetIdentifier sheet) {
            requireKnown(sheet);
            return workbook.get3DReferencePtg(cell, sheet);
        }

        @Override
        public Ptg get3DReferencePtg(AreaReference area, SheetIdentifier sheet) {
            requireKnown(sheet);
            return workbook.get3DReferencePtg(area, sheet);
        }

        private void requireKnown(@Nullable SheetIdentifier sheet) {
            if (sheet == null || sheet.getBookName() != null) {
                return;
            }
            var names = sheet instanceof SheetRangeIdentifier range
                    ? List.of(range.getFirstSheetIdentifier(), range.getLastSheetIdentifier())
                    : List.of(sheet.getSheetIdentifier());
            for (var name : names) {
                if (!hasSheet.test(name.getName())) {
                    throw new FormulaParseException("The workbook has no sheet '" + name.getName() + "'.");
                }
            }
        }
    }

    /**
     * What the parser of a formula asks the workbook for when the formula names a sheet.
     */
    private interface SheetReferences {

        Ptg getNameXPtg(String name, SheetIdentifier sheet);

        Ptg get3DReferencePtg(CellReference cell, SheetIdentifier sheet);

        Ptg get3DReferencePtg(AreaReference area, SheetIdentifier sheet);
    }
}
