package org.openl.rules.testmethod;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.jspecify.annotations.Nullable;

import org.openl.rules.convertor.NumberNotation;
import org.openl.rules.table.ICell;
import org.openl.rules.table.ui.ICellStyle;
import org.openl.rules.table.xls.formatters.FormatConstants;
import org.openl.rules.testmethod.result.Tolerance;

/**
 * Reads the figures an expected number is written with in a cell of a test table.
 * <p>
 * A text cell is read as it is typed, and a number cell as its number format shows it: {@code 10} formatted with
 * {@code 0.00} reads {@code 10.00}, and with {@code $#,##0.00} reads {@code $10.00}, whose number is {@code 10.00}.
 * Grouping commas are skipped, a percent is a hundredth, and an exponent keeps the figures of its mantissa:
 * {@code 1.00E+03} has three figures.
 * <p>
 * A fraction format, such as {@code # ?/?} or {@code # ?/16}, shows the number to one part of its denominator: a cell
 * showing {@code 1 1/2} has the denominator 2, and a whole number shown without a fraction the denominator 1 or the one
 * the format fixes. Only the section of the format that shows the value counts, and a text in quotes is no fraction:
 * {@code 0.00;# ?/16} shows {@code 1.25} to hundredths, and {@code "?/8 "0.00} as well.
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

    // The slash of a fraction format, after a digit placeholder, such as in # ?/? or # ?\ /?
    private static final Pattern FRACTION = Pattern.compile("[?#]\\s*/");

    // The denominator a fraction format fixes, such as 16 in # ?/16 or # ?/016
    private static final Pattern FIXED_DENOMINATOR = Pattern.compile("[?#]\\s*/\\s*(\\d{1,9})(?![\\d?#])");

    // The denominator a fraction shows, such as 2 in 1 1/2
    private static final Pattern SHOWN_DENOMINATOR = Pattern.compile("/(\\d{1,9})");

    // A text a format shows as it is: in quotes, or a character after a backslash
    private static final Pattern LITERAL = Pattern.compile("\"[^\"]*\"?|\\\\.");

    private WrittenFigures() {
    }

    /**
     * Returns the figures a cell shows its number with: the decimal place of the last figure, such as 2 for
     * {@code 10.00}, or the denominator of a fraction, such as 2 for {@code 1 1/2}.
     *
     * @return the figures, or {@code null} when the cell shows no number with figures of its own
     */
    static Tolerance.@Nullable Figures figuresOf(ICell cell, DataFormatter formatter) {
        return switch (cell.getObjectValue()) {
            case String typed -> expectedFigures(valueOf(typed));
            case Number number -> shownFigures(number.doubleValue(), cell.getStyle(), formatter);
            case null, default -> null;
        };
    }

    private static Tolerance.@Nullable Figures expectedFigures(@Nullable BigDecimal written) {
        return written == null ? null : new Tolerance.ExpectedFigures(written.scale());
    }

    private static Tolerance.@Nullable Figures shownFigures(double value,
                                                            @Nullable ICellStyle style,
                                                            DataFormatter formatter) {
        if (style == null || !Double.isFinite(value)) {
            return null;
        }
        var format = style.getFormatString();
        if (format == null || format.isEmpty() || FormatConstants.GENERAL_FORMAT.equalsIgnoreCase(format)) {
            return null;
        }
        var formatIndex = style.getFormatIndex();
        var section = FormatSections.sectionFor(format, value);
        var shownBy = Objects.requireNonNullElse(section, format);
        var codes = LITERAL.matcher(shownBy).replaceAll(" ");
        return isFraction(codes)
                ? new Tolerance.ExpectedFraction(denominatorOf(value, shownBy, codes, formatIndex, formatter))
                : expectedFigures(shown(value, section, shownBy, formatIndex, formatter));
    }

    /**
     * Tells whether the codes of a format section show a number as a fraction: a {@code ?} or a {@code #} before a
     * slash, maybe with spaces between, such as in {@code # ?/?} or {@code # ?\ /?}.
     */
    private static boolean isFraction(String codes) {
        return FRACTION.matcher(codes).find();
    }

    /**
     * Returns the denominator of a fraction: the one the section fixes, such as 16 for {@code # ?/16}, or else the one
     * the cell shows, such as 2 for {@code 1 1/2}, apart from a text in quotes. A whole number shown without a fraction
     * has the denominator 1.
     */
    private static int denominatorOf(double value,
                                     String section,
                                     String codes,
                                     short formatIndex,
                                     DataFormatter formatter) {
        var fixed = FIXED_DENOMINATOR.matcher(codes);
        var fixedDenominator = fixed.find() ? Integer.parseInt(fixed.group(1)) : 0;
        if (fixedDenominator > 0) {
            return fixedDenominator;
        }
        var shown = formatter.formatRawCellContents(value, formatIndex, blankLiterals(section));
        var denominator = SHOWN_DENOMINATOR.matcher(shown).results().reduce((first, second) -> second);
        return denominator.map(match -> Math.max(Integer.parseInt(match.group(1)), 1)).orElse(1);
    }

    /**
     * Blanks the texts a format section shows as they are, keeping their place: {@code # ?/?" /8"} becomes
     * {@code # ?/?"   "}, so that the section shows no number but the fraction.
     */
    private static String blankLiterals(String section) {
        return LITERAL.matcher(section)
                .replaceAll(literal -> Matcher.quoteReplacement(literal.group().startsWith("\"")
                        ? literal.group().replaceAll("[^\"]", " ")
                        : "\\ "));
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
     * The value is shown by the {@code section} of the format that shows it, and by the whole format {@code shownBy}
     * when no section does.
     * <p>
     * Among the numbers the format shows, such as {@code 2} and {@code 10.00} in {@code CO2: 10.00}, the cell shows its
     * value in the one that is the value times a power of ten, and in the one with more figures on a tie.
     * <p>
     * A value shown as zero takes the power of ten from the first of 1, 1000, a million and so on that the section of
     * the format showing the value shows with figures. So {@code 0} shown by {@code 0.00,,,,} reads {@code 0E+10}, and
     * by the zero section of {@code [<1]0.00,;0.00} reads {@code 0E+1}.
     */
    private static @Nullable BigDecimal shown(double value,
                                              @Nullable String section,
                                              String shownBy,
                                              short formatIndex,
                                              DataFormatter formatter) {
        var shown = numbersShown(formatter.formatRawCellContents(value, formatIndex, shownBy));
        var scaling = scalingOf(shown, value);
        if (scaling == null && section != null) {
            scaling = sectionScaling(section, formatIndex, shown.size(), formatter);
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
