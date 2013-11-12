package org.apache.poi.ss.usermodel;

import java.util.Locale;
import junit.framework.TestCase;

public class DataFormatterTest extends TestCase {

    public void testFormatRawCellContentsAccountingFormat() {
        DataFormatter formatter = new DataFormatter(Locale.US);
        assertEquals("-0.002000000р.", formatter.formatRawCellContents(-0.002, 175,
                "_-* #,##0.000000000\"р.\"_-;\\-* #,##0.000000000\"р.\"_-;_-* \"-\"?????????\"р.\"_-;_-@_-"));
        assertEquals("$1.234567890", formatter.formatRawCellContents(1.23456789, 166,
                "_-[$$-1009]* #,##0.000000000_-;\\-[$$-1009]* #,##0.000000000_-;_-[$$-1009]* \"-\"?????????_-;_-@_-"));
        assertEquals(" 700,000.00р.", formatter.formatRawCellContents(700000.0, 44,
                "_-* #,##0.00\"р.\"_-;\\-* #,##0.00\"р.\"_-;_-* \"-\"??\"р.\"_-;_-@_-"));
        assertEquals(" 750.06р.", formatter.formatRawCellContents(750.06, 183,
                "_-* #,##0.00[$р.-419]_-;\\-* #,##0.00[$р.-419]_-;_-* \"-\"??[$р.-419]_-;_-@_-"));
        assertEquals(" 760.12 ман.", formatter.formatRawCellContents(760.123000003397, 184,
                "_-* #,##0.00\\ [$ман.-82C]_-;\\-* #,##0.00\\ [$ман.-82C]_-;_-* \"-\"??\\ [$ман.-82C]_-;_-@_-"));
        assertEquals("900.1222222", formatter.formatRawCellContents(900.1222222, 167,
                "_-* #,##0.00000000000000_р_._-;\\-* #,##0.00000000000000_р_._-;_-* \"-\"??????????????_р_._-;_-@_-"));
        assertEquals("€ 1,000.00", formatter.formatRawCellContents(1000.0, 168,
                "_-[$€-2]\\ * #,##0.00_-;\\-[$€-2]\\ * #,##0.00_-;_-[$€-2]\\ * \"-\"??_-;_-@_-"));
        assertEquals("10,000.00 [$USD]", formatter.formatRawCellContents(10000.0, 172, "#,##0.00\\ [$USD]"));
    }

    public void testFormatRawCellContentsCurrencyFormat() {
        DataFormatter formatter = new DataFormatter(Locale.US);
        assertEquals("-0.00100000 р.", formatter.formatRawCellContents(-0.001, 176, "#,##0.00000000\\ [$р.-423]"));
        assertEquals("1.234567890р.",
                formatter.formatRawCellContents(1.23456789, 185, "#,##0.000000000\"р.\";\\-#,##0.000000000\"р.\""));
        assertEquals("700,000.00 лв.",
                formatter.formatRawCellContents(700000.0, 186, "#,##0.00\\ [$лв.-402];\\-#,##0.00\\ [$лв.-402]"));
        assertEquals("900.1222222", formatter.formatRawCellContents(900.1222222, 188,
                "#,##0.00000000000000_р_.;\\-#,##0.00000000000000_р_."));
    }

    public void testFormatRawCellContentsFractionFormat() {
        DataFormatter formatter = new DataFormatter();
        assertEquals("0 2/5", formatter.formatRawCellContents(0.49, 178, "# ?/5"));
        assertEquals("0", formatter.formatRawCellContents(-0.0, 178, "# ?/5"));
        assertEquals("-1 3/5", formatter.formatRawCellContents(-1.5, 180, "# ??/5"));
        assertEquals("-0 2/7", formatter.formatRawCellContents(-0.3, 12, "# ?/?"));
        assertEquals("-1 23/59", formatter.formatRawCellContents(-1.39, 13, "# ??/??"));
        assertEquals("1 253/769", formatter.formatRawCellContents(1.329, 182, "# ???/???"));
        assertEquals("-11 2/4", formatter.formatRawCellContents(-11.5, 183, "# ?/4"));
        assertEquals("-0 2/8", formatter.formatRawCellContents(-0.3, 179, "# ?/8"));
        assertEquals("0 8/16", formatter.formatRawCellContents(0.49, 184, "# ??/16"));
        assertEquals("1 3/10", formatter.formatRawCellContents(1.329, 185, "# ?/10"));
        assertEquals("-11 50/100", formatter.formatRawCellContents(-11.5, 186, "# ??/100"));
        assertEquals("-0 2/3", formatter.formatRawCellContents(-0.5, 187, "# ?/3"));
    }

}
