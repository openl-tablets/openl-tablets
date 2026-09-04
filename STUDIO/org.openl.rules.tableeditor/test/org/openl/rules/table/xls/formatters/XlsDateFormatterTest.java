package org.openl.rules.table.xls.formatters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import org.junit.jupiter.api.Test;

class XlsDateFormatterTest {

    private final XlsDateFormatter formatter = new XlsDateFormatter(FormatConstants.DEFAULT_XLS_DATE_FORMAT);

    @Test
    void parsesDateUsingTheCellFormatFirst() {
        var expected = Date.from(LocalDate.of(2024, 1, 2).atStartOfDay(ZoneId.systemDefault()).toInstant());

        assertEquals(expected, formatter.parse("1/2/24"));
    }

    @Test
    void parsesRawApiIsoDateWhenItDoesNotMatchTheCellFormat() {
        var expected = Date.from(LocalDateTime.of(2024, 1, 2, 3, 4, 5, 678_000_000)
                .atZone(ZoneId.systemDefault())
                .toInstant());

        assertEquals(expected, formatter.parse("2024-01-02T03:04:05.678"));
    }

    @Test
    void keepsInvalidDateUnparsed() {
        assertNull(formatter.parse("not-a-date"));
    }
}
