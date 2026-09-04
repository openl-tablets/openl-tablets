package org.openl.rules.table.xls.writers;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.function.Function;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Date1904Support;
import org.apache.poi.ss.usermodel.DateUtil;
import org.jspecify.annotations.Nullable;

import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.rules.table.xls.formatters.FormatConstants;
import org.openl.util.StringUtils;

public class XlsCellArrayWriter extends AXlsCellWriter {

    private static final DateTimeFormatter GENERAL_DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");

    // XlsDateFormatter parses textual Excel dates with the JVM default locale.
    private final DataFormatter dataFormatter = new DataFormatter(Locale.getDefault());

    public XlsCellArrayWriter(XlsSheetGridModel xlsSheetGridModel) {
        super(xlsSheetGridModel);
    }

    @Override
    public void writeCellValue() {
        getCellToWrite().setCellValue(serialize((Object[]) getValueToWrite(), this::serializeElement));
    }

    /**
     * Converts an array to the workbook text parsed by OpenL array readers.
     *
     * <p>A separator following a trailing backslash is padded with whitespace. The parser removes that padding but
     * does not mistake the separator for an escaped comma.
     */
    public static String serialize(@Nullable Object[] values) {
        return serialize(values, XlsCellArrayWriter::serializeElementWithoutCellFormat);
    }

    private static String serialize(@Nullable Object[] values, Function<@Nullable Object, String> serializer) {
        var elements = Arrays.stream(values).map(serializer).toArray(String[]::new);
        for (var i = 0; i < elements.length - 1; i++) {
            if (elements[i].endsWith("\\")) {
                elements[i] += StringUtils.SPACE;
            }
        }
        return String.join(",", elements);
    }

    private String serializeElement(@Nullable Object value) {
        var text = value instanceof Date date ? formatDate(date) : value == null ? StringUtils.EMPTY : value.toString();
        return escapeSeparator(text);
    }

    private String formatDate(Date value) {
        var style = getCellToWrite().getCellStyle();
        var formatIndex = style.getDataFormat();
        var format = style.getDataFormatString();
        if (StringUtils.isBlank(format) || FormatConstants.GENERAL_FORMAT.equals(format)) {
            return GENERAL_DATE_TIME_FORMATTER.format(value.toInstant().atZone(ZoneId.systemDefault()));
        }
        var use1904Windowing = uses1904DateWindowing();
        return dataFormatter.formatRawCellContents(DateUtil.getExcelDate(value, use1904Windowing),
                formatIndex,
                format,
                use1904Windowing);
    }

    private boolean uses1904DateWindowing() {
        var workbook = getCellToWrite().getSheet().getWorkbook();
        if (workbook instanceof Date1904Support date1904Support) {
            return date1904Support.isDate1904();
        }
        return workbook instanceof HSSFWorkbook hssfWorkbook
                && hssfWorkbook.getInternalWorkbook().isUsing1904DateWindowing();
    }

    private static String serializeElementWithoutCellFormat(@Nullable Object value) {
        return escapeSeparator(value == null ? StringUtils.EMPTY : value.toString());
    }

    private static String escapeSeparator(String value) {
        return value.replace(",", "\\,");
    }
}
