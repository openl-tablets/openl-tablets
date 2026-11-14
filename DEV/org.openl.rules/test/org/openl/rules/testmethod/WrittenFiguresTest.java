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
        return WrittenFigures.scaleOf(cell, new DataFormatter(Locale.US));
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
