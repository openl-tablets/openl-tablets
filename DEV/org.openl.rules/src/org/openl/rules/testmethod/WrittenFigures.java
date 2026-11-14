package org.openl.rules.testmethod;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

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

    // The largest number the scaling of a format is looked for with, far beyond the thousands any format divides by
    private static final double MAX_REFERENCE = 1E300;

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
     * Reads the number a number cell shows through its number format, as a figure of the value: {@code 1.23} shown by
     * {@code 0.00,} for {@code 1234.5}, in thousands, reads {@code 1.23E+3}, and {@code 12.50 %} shown by
     * {@code 0.00 %} for {@code 0.125} reads {@code 0.1250}.
     * <p>
     * Among the numbers the format shows, such as {@code 2} and {@code 10.00} in {@code CO2: 10.00}, the cell shows its
     * value in the one that is the value times a power of ten, and in the one with more figures on a tie.
     * <p>
     * A value shown as zero takes the power of ten from the first of 1, 1000, a million and so on that the section of
     * the format showing the value shows with figures. So {@code 0} shown by {@code 0.00,,,,} reads {@code 0E+10}, and
     * by the zero section of {@code [<1]0.00,;0.00} reads {@code 0E+1}.
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
        var text = formatter.formatRawCellContents(value, style.getFormatIndex(), format);
        var shown = numbersShown(text);
        var scaling = scalingOf(shown, value);
        if (scaling == null) {
            var section = FormatSections.sectionFor(format, value);
            scaling = section == null ? null : sectionScaling(section, style.getFormatIndex(), shown.size(), formatter);
        }
        var written = scaling == null ? null : shown.get(scaling.index());
        return written == null ? null : written.scaleByPowerOfTen(-scaling.power());
    }

    /**
     * Finds the power of ten a section of a format shows numbers with, from the first of 1, 1000, a million and so on
     * that it shows with figures in as many numbers as the value.
     */
    private static @Nullable Scaling sectionScaling(String section,
                                                    short formatIndex,
                                                    int numbers,
                                                    DataFormatter formatter) {
        return DoubleStream.iterate(1, reference -> reference <= MAX_REFERENCE, reference -> reference * 1000)
                .mapToObj(reference -> {
                    var shown = numbersShown(formatter.formatRawCellContents(reference, formatIndex, section));
                    return shown.size() == numbers ? scalingOf(shown, reference) : null;
                })
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    /**
     * Reads the numbers a format shows, one for each number in the text, or {@code null} for a text that is no number.
     * The numbers carry no sign, as a negative number may be shown in parentheses. Every comma in them groups digits,
     * even among the leading zeros of {@code 000,010.00}.
     */
    private static List<@Nullable BigDecimal> numbersShown(String shown) {
        return SHOWN_NUMBER.matcher(shown).results().map(match -> valueOf(match.group().replace(",", ""))).toList();
    }

    /**
     * Finds the shown number that is the value times a power of ten.
     *
     * @return the index of the number and the power, or {@code null} when the value or every shown number is zero
     */
    private static @Nullable Scaling scalingOf(List<@Nullable BigDecimal> shown, double value) {
        if (value == 0) {
            return null;
        }
        return IntStream.range(0, shown.size())
                .mapToObj(i -> scalingOf(i, shown.get(i), value))
                .filter(Objects::nonNull)
                .min(Comparator.comparingDouble(Scaling::distance)
                        .thenComparing(Scaling::figures, Comparator.reverseOrder()))
                .orElse(null);
    }

    private static @Nullable Scaling scalingOf(int index, @Nullable BigDecimal shown, double value) {
        if (shown == null || shown.signum() == 0) {
            return null;
        }
        var power = Math.log10(shown.doubleValue() / Math.abs(value));
        return new Scaling(index, (int) Math.round(power), Math.abs(power - Math.round(power)), shown.precision());
    }

    /**
     * A shown number, by its {@code index}, that is the value times ten to the {@code power}, within {@code distance}
     * of a power of ten, and shows that many {@code figures}.
     */
    private record Scaling(int index, int power, double distance, int figures) {
    }
}
