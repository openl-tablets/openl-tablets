package org.openl.rules.testmethod;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.Locale;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.openl.rules.table.ICell;
import org.openl.rules.table.ui.ICellStyle;
import org.openl.rules.testmethod.result.Tolerance;

/**
 * @author Yury Molchan
 */
class WrittenFiguresTest {

    @Test
    void writtenNumber() {
        assertEquals(2, WrittenFigures.scaleOf("10.00"));
        assertEquals(3, WrittenFigures.scaleOf("10.000"));
        assertEquals(4, WrittenFigures.scaleOf("0.0001"));
        assertEquals(0, WrittenFigures.scaleOf("1000"));
        assertEquals(0, WrittenFigures.scaleOf(" 7 "));
        assertEquals(1, WrittenFigures.scaleOf("-3.0"));
        assertEquals(1, WrittenFigures.scaleOf("+.5"));
        assertEquals(0, WrittenFigures.scaleOf("5."));
    }

    @Test
    void groupingPercentAndExponent() {
        assertEquals(2, WrittenFigures.scaleOf("1,000.50"));
        assertEquals(3, WrittenFigures.scaleOf("12.5%"));
        assertEquals(-1, WrittenFigures.scaleOf("1.00E+03"));
        assertEquals(9, WrittenFigures.scaleOf("1.331e-6"));
    }

    @Test
    void notOneNumber() {
        assertNull(WrittenFigures.scaleOf(""));
        assertNull(WrittenFigures.scaleOf("abc"));
        assertNull(WrittenFigures.scaleOf("%"));
        assertNull(WrittenFigures.scaleOf("1.0, 2.0"));
        assertNull(WrittenFigures.scaleOf("$10.00"));
        // commas that do not group thousands
        assertNull(WrittenFigures.scaleOf("12,34.00"));
        assertNull(WrittenFigures.scaleOf("0,500"));
    }

    @Test
    void longTextIsReadInLinearTime() {
        var digits = "9".repeat(100_000);
        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
            assertNull(WrittenFigures.scaleOf(digits + "x"));
            assertEquals(0, WrittenFigures.scaleOf(digits));
        });
    }

    @Test
    void textCell() {
        assertEquals(2, scaleOf(cell("10.00", null)));
        assertNull(scaleOf(cell("ten", null)));
    }

    @Test
    void numberCellAsItsFormatShowsIt() {
        assertEquals(2, scaleOf(cell(10.0, style(2, "0.00"))));
        assertEquals(3, scaleOf(cell(1234.5, style(4, "#,##0.000"))));
        assertEquals(4, scaleOf(cell(0.125, style(10, "0.00%"))));
        assertEquals(-1, scaleOf(cell(1234.5, style(11, "0.00E+00"))));
    }

    @Test
    void numberAmongWhatTheFormatAdds() {
        assertEquals(2, scaleOf(cell(1234.5, style(7, "$#,##0.00"))));
        assertEquals(2, scaleOf(cell(10.0, style(164, "0.00 \"kg\""))));
        assertEquals(2, scaleOf(cell(-10.0, style(165, "0.00;(0.00)"))));
        assertEquals(4, scaleOf(cell(0.125, style(10, "0.00%"))));
        // a number the format adds is not the value of the cell
        assertEquals(2, scaleOf(cell(10.0, style(166, "\"CO2: \"0.00"))));
        assertEquals(2, scaleOf(cell(2.0, style(166, "\"CO2: \"0.00"))));
        assertEquals(2, scaleOf(cell(10.0, style(167, "0.00\" m2\""))));
    }

    @Test
    void numberTheFormatScales() {
        // a percent apart from the number is a hundredth all the same
        assertEquals(4, scaleOf(cell(0.125, style(168, "0.00 %"))));
        // each comma after the digits shows thousands
        assertEquals(-1, scaleOf(cell(1234.5, style(169, "0.00,"))));
        assertEquals(-4, scaleOf(cell(1234567.0, style(170, "0.00,,"))));
        // a value shown as zero takes the scaling of the format
        assertEquals(-1, scaleOf(cell(0.0, style(169, "0.00,"))));
        assertEquals(2, scaleOf(cell(0.001, style(2, "0.00"))));
        assertEquals(3, scaleOf(cell(-0.0, style(171, "0.000;(0.000)"))));
        // a format that shows even a million as zero
        assertEquals(-10, scaleOf(cell(1000000.0, style(172, "0.00,,,,"))));
        assertEquals(-10, scaleOf(cell(0.0, style(172, "0.00,,,,"))));
        // the numbers the scaling is looked for with are shown by the section of the value
        assertEquals(2, scaleOf(cell(0.0, style(173, "[>=1000]0.0,;0.00"))));
        assertEquals(2, scaleOf(cell(0.0, style(174, "[<1]0.00;0.0,"))));
        assertEquals(-1, scaleOf(cell(0.0, style(176, "[<1]0.00,;0.00"))));
        assertEquals(-1, scaleOf(cell(0.0, style(177, "0.00;(0.00);0.00,"))));
        assertEquals(-1, scaleOf(cell(0.0, style(178, "\"[<1]x\"0.00;0.00;0.00,"))));
        assertEquals(-1, scaleOf(cell(0.0, style(179, "[<.5]0.00,;0.00"))));
        assertEquals(2, scaleOf(cell(0.0, style(180, "[>=1000]0.0,;0.00,;0.00"))));
        assertEquals(2, scaleOf(cell(-0.001, style(2, "0.00"))));
        // leading zeros grouped by the format
        assertEquals(2, scaleOf(cell(10.0, style(175, "000,000.00"))));
        assertEquals(2, scaleOf(cell(0.0, style(175, "000,000.00"))));
    }

    @Test
    void fractionShownByTheFormat() {
        // the denominator the cell shows
        assertEquals(fraction(2), figuresOf(cell(1.5, style(181, "# ?/?"))));
        assertEquals(fraction(4), figuresOf(cell(0.25, style(181, "# ?/?"))));
        assertEquals(fraction(2), figuresOf(cell(-1.5, style(181, "# ?/?"))));
        assertEquals(fraction(4), figuresOf(cell(2.75, style(182, "?/?"))));
        assertEquals(fraction(13), figuresOf(cell(12.0 / 13, style(183, "# ##/##"))));
        assertEquals(fraction(2), figuresOf(cell(1.5, style(184, "# ?/? \"kg\""))));
        // a whole number shows no fraction
        assertEquals(fraction(1), figuresOf(cell(2.0, style(181, "# ?/?"))));
        assertEquals(fraction(1), figuresOf(cell(0.01, style(181, "# ?/?"))));
        // the denominator the format fixes, even for a whole number
        assertEquals(fraction(16), figuresOf(cell(1.5, style(185, "# ?/16"))));
        assertEquals(fraction(16), figuresOf(cell(2.0, style(185, "# ?/16"))));
        assertEquals(fraction(100), figuresOf(cell(0.25, style(186, "# ??/100"))));
        // Apache POI reads 016 as 16, and shows a fraction with spaces before the slash
        assertEquals(fraction(16), figuresOf(cell(2.0, style(197, "# ?/016"))));
        assertEquals(fraction(16), figuresOf(cell(2.0, style(198, "# ?/ 16"))));
        assertEquals(fraction(2), figuresOf(cell(1.5, style(199, "# ?\\ /?"))));
        // a denominator a format cannot fix is the one the cell shows: Apache POI shows hundredths for # ?/0
        assertEquals(fraction(100), figuresOf(cell(1.5, style(187, "# ?/0"))));
        assertNull(figuresOf(cell(Double.NaN, style(181, "# ?/?"))));
    }

    @Test
    void onlyTheSectionShowingTheValueCounts() {
        // 1.25 is shown by 0.00, and -1.25 by # ?/16
        assertEquals(2, scaleOf(cell(1.25, style(188, "0.00;# ?/16"))));
        assertEquals(fraction(16), figuresOf(cell(-1.25, style(188, "0.00;# ?/16"))));
        assertEquals(fraction(4), figuresOf(cell(-1.25, style(189, "0.00;# ?/?"))));
        // a percent of the negative section, too
        assertEquals(4, scaleOf(cell(0.125, style(190, "0.00%;-0.00%"))));
        assertEquals(4, scaleOf(cell(-0.125, style(191, "0.00%;(0.00%)"))));
        assertEquals(2, scaleOf(cell(0.125, style(192, "0%;[Red]-0%"))));
    }

    @Test
    void textInQuotesIsNoFraction() {
        assertEquals(2, scaleOf(cell(1.25, style(193, "\"?/8 \"0.00"))));
        assertEquals(2, scaleOf(cell(1.25, style(194, "0.00\" ?/8\""))));
        assertEquals(fraction(8), figuresOf(cell(1.25, style(195, "# ?/8\" kg\""))));
        assertEquals(fraction(4), figuresOf(cell(1.25, style(196, "\"x/16 \"# ?/?"))));
        // Apache POI shows 1 1/2 /8, and the fraction is 1 1/2
        assertEquals(fraction(2), figuresOf(cell(1.5, style(200, "#\" \"?/?\" /8\""))));
        assertEquals(fraction(2), figuresOf(cell(1.5, style(201, "# ?/?\\/\\8"))));
    }

    @Test
    void exponentBeyondTheDecimalPlaces() {
        assertNull(WrittenFigures.scaleOf("1E99999999999"));
        assertNull(WrittenFigures.scaleOf("1E-2147483647%"));
    }

    @Test
    void numberCellWithoutFigures() {
        assertNull(scaleOf(cell(10.0, style(0, "General"))));
        assertNull(scaleOf(cell(10.0, style(0, ""))));
        assertNull(scaleOf(cell(10.0, null)));
        assertNull(scaleOf(cell(true, null)));
        assertNull(scaleOf(cell(null, null)));
    }

    private static @Nullable Integer scaleOf(ICell cell) {
        return figuresOf(cell) instanceof Tolerance.ExpectedFigures(Integer scale) ? scale : null;
    }

    private static Tolerance.@Nullable Figures figuresOf(ICell cell) {
        return WrittenFigures.figuresOf(cell, new DataFormatter(Locale.US));
    }

    private static Tolerance fraction(int denominator) {
        return new Tolerance.ExpectedFraction(denominator);
    }

    private static ICell cell(@Nullable Object value, @Nullable ICellStyle style) {
        return (ICell) Proxy.newProxyInstance(ICell.class.getClassLoader(), new Class<?>[]{ICell.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getObjectValue" -> value;
                    case "getStyle" -> style;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private static ICellStyle style(int index, String format) {
        return (ICellStyle) Proxy.newProxyInstance(ICellStyle.class.getClassLoader(),
                new Class<?>[]{ICellStyle.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getFormatIndex" -> (short) index;
                    case "getFormatString" -> format;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }
}
