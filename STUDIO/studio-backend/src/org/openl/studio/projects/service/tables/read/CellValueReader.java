package org.openl.studio.projects.service.tables.read;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.function.Function;

import lombok.RequiredArgsConstructor;

import org.openl.rules.lang.xls.types.meta.MetaInfoReader;
import org.openl.rules.table.ICell;
import org.openl.rules.table.xls.formatters.XlsDataFormatterFactory;
import org.openl.util.NumberUtils;

/**
 * Reads the value of a cell as the table API answers it.
 *
 * <p>A text in a cell of a typed value is read as a value of that type, such as a number or a date, when it reads as
 * one. A date is answered without a time of the day when it has none.
 *
 * <p>A text of a number that a {@code double} does not hold exactly, such as
 * {@code 0.6666666666666666666666666666666667}, stays a text. A cell keeps such a number so, and a client reading a
 * JSON number as a {@code double} would lose its last digits.
 */
@RequiredArgsConstructor
public class CellValueReader implements Function<ICell, Object> {

    private final MetaInfoReader metaInfoReader;

    @Override
    public Object apply(ICell cell) {
        var value = cell.getObjectValue();
        if (value instanceof String string) {
            var metaInfo = metaInfoReader.getMetaInfo(cell.getAbsoluteRow(), cell.getAbsoluteColumn());
            var formatter = XlsDataFormatterFactory.getFormatter(cell, metaInfo);
            if (formatter != null) {
                var parsedValue = formatter.parse(string);
                if (parsedValue != null && !(parsedValue instanceof Number && NumberUtils.beyondDouble(string) != null)) {
                    value = parsedValue;
                }
            }
        }
        if (value instanceof Date date) {
            // If the time is not set, then return LocalDate, otherwise LocalDateTime
            var zonedDateTime = date.toInstant().atZone(ZoneId.systemDefault());
            if (zonedDateTime.toLocalTime().isAfter(LocalTime.MIN)) {
                value = zonedDateTime.toLocalDateTime();
            } else {
                value = zonedDateTime.toLocalDate();
            }
        }
        return value;
    }

}
