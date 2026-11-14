package org.openl.rules.testmethod;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Objects;
import java.util.regex.Pattern;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.jspecify.annotations.Nullable;

import org.openl.rules.convertor.NumberNotation;
import org.openl.rules.table.ICell;
import org.openl.rules.table.xls.formatters.FormatConstants;

/**
 * Reads the figures an expected number is written with in a cell of a test table.
 * <p>
 * A text cell is read as it is typed, and a number cell as its number format shows it: {@code 10} formatted with
 * {@code 0.00} reads {@code 10.00}, and with {@code $#,##0.00} reads {@code $10.00}, whose number is {@code 10.00}.
 * Grouping commas are skipped, a percent is a hundredth, and an exponent keeps the figures of its mantissa:
 * {@code 1.00E+03} has three figures.
 * <p>
 * The General format shows no more figures than the number has, so such a cell has no written figures, nor has a cell
 * that holds more than one number.
 *
 * @author Yury Molchan
 */
final class WrittenFigures {

    private static final Pattern NUMBER = Pattern.compile("[-+]?(\\d+(\\.\\d*)?|\\.\\d+)([eE][-+]?\\d+)?%?");

    // The number a format shows among what it adds, such as a currency, a unit or the parentheses of a negative
    private static final Pattern SHOWN_NUMBER = Pattern.compile("(\\d[\\d,]*(\\.\\d*)?|\\.\\d+)([eE][-+]?\\d+)?%?");

    private WrittenFigures() {
    }

    /**
     * Returns the decimal place of the last figure a cell shows, such as 2 for {@code 10.00}.
     *
     * @return the decimal place, or {@code null} when the cell shows no number with figures of its own
     */
    static @Nullable Integer scaleOf(ICell cell, DataFormatter formatter) {
        var written = switch (cell.getObjectValue()) {
            case String typed -> valueOf(typed);
            case Number number -> shown(number, cell, formatter);
            case null, default -> null;
        };
        return written == null ? null : written.scale();
    }

    /**
     * Returns the decimal place of the last figure of a written number, such as 2 for {@code 10.00} and -1 for
     * {@code 1.00E+03}.
     *
     * @return the decimal place, or {@code null} when the text is not one number
     */
    static @Nullable Integer scaleOf(String text) {
        var written = valueOf(text);
        return written == null ? null : written.scale();
    }

    /**
     * Reads a written number with the figures it is written with. A percent is a hundredth.
     *
     * @return the number, or {@code null} when the text is not one number
     */
    private static @Nullable BigDecimal valueOf(String text) {
        var number = NumberNotation.removeThousandsSeparators(text.strip());
        if (!NUMBER.matcher(number).matches()) {
            return null;
        }
        try {
            return number.endsWith("%")
                    ? new BigDecimal(number.substring(0, number.length() - 1)).scaleByPowerOfTen(-2)
                    : new BigDecimal(number);
        } catch (NumberFormatException | ArithmeticException e) {
            // an exponent beyond the range of a decimal place
            return null;
        }
    }

    /**
     * Reads the number a number cell shows through its number format. Among the numbers the format shows, such as
     * {@code 2} and {@code 10.00} in {@code CO2: 10.00}, the cell shows its value in the one nearest to the value, and
     * in the one with more figures on a tie.
     */
    private static @Nullable BigDecimal shown(Number number, ICell cell, DataFormatter formatter) {
        var style = cell.getStyle();
        var value = number.doubleValue();
        if (style == null || !Double.isFinite(value)) {
            return null;
        }
        var format = style.getFormatString();
        if (format == null || format.isEmpty() || FormatConstants.GENERAL_FORMAT.equalsIgnoreCase(format)) {
            return null;
        }
        var shown = formatter.formatRawCellContents(value, style.getFormatIndex(), format);
        // the shown numbers carry no sign: a negative number may be shown in parentheses
        var magnitude = BigDecimal.valueOf(Math.abs(value));
        return SHOWN_NUMBER.matcher(shown)
                .results()
                .map(match -> valueOf(match.group()))
                .filter(Objects::nonNull)
                .min(Comparator.comparing((BigDecimal written) -> written.subtract(magnitude).abs())
                        .thenComparing(BigDecimal::scale, Comparator.reverseOrder()))
                .orElse(null);
    }
}
