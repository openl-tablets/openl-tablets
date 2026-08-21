package org.openl.studio.projects.service.tables;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

import org.openl.rules.convertor.String2DataConvertorFactory;
import org.openl.rules.table.formatters.FormattersManager;
import org.openl.rules.table.properties.TableProperties;
import org.openl.rules.table.properties.def.TablePropertyDefinitionUtils;

/**
 * The text a table property's value is written as on the copy API, and the value such a text stands for.
 *
 * <p>A date crosses the wire in ISO-8601 — the day alone, with the time after it only when the value carries one —
 * so the same text is read the same way at both ends whatever the reader's locale. A property closing a period,
 * which the engine keeps at the close of the day it names, crosses as that day. Every other value crosses in the
 * display form its definition gives, the one the Table Details editor shows.
 *
 * <p>A value is read back as the property it names: a date, a flag, an enumeration or a list of them, the way the
 * engine reads the cell it is written to. A text the property cannot be read from is kept as it stands, so a value
 * an author typed is written rather than dropped.
 *
 * @author Vladyslav Pikus
 */
@Slf4j
final class TablePropertyText {

    private TablePropertyText() {
    }

    /**
     * The value as the copy API writes it.
     *
     * @param name  name of the property the value belongs to
     * @param value the value the table declares
     * @return the text the value is written as, or {@code null} when there is no value
     */
    static @Nullable String format(String name, @Nullable Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Date date) {
            // A property closing a period stands for the end of its day, which is the engine's moment rather than
            // a value the author wrote: the day it names is what crosses, the way Table Details shows it.
            return TableProperties.END_OF_DAY_PROPERTIES.contains(name) ? isoDay(date) : iso(date);
        }
        var definition = TablePropertyDefinitionUtils.getPropertyByName(name);
        if (definition == null) {
            return String.valueOf(value);
        }
        return FormattersManager.getFormatter(definition.getType().getInstanceClass(), definition.getFormat())
                .format(value);
    }

    /**
     * The value a declared text stands for.
     *
     * @param name name of the property the text was declared for
     * @param text the text declared for it
     * @return the value it is read as, or the text itself when the property cannot be read from it
     */
    static Object parse(String name, String text) {
        var definition = TablePropertyDefinitionUtils.getPropertyByName(name);
        if (definition == null) {
            return text;
        }
        var type = definition.getType().getInstanceClass();
        try {
            // Formatters answer null for a text they cannot read, where the engine's own reader throws.
            var value = type == Date.class
                    ? String2DataConvertorFactory.parse(Date.class, text, null)
                    : FormattersManager.getFormatter(type, definition.getFormat()).parse(text);
            return value == null ? text : value;
        } catch (RuntimeException e) {
            log.debug("Cannot read property '{}' from '{}'.", name, text, e);
            return text;
        }
    }

    /** The day a date falls on, in ISO-8601. */
    private static String isoDay(Date value) {
        return LocalDateTime.ofInstant(value.toInstant(), ZoneId.systemDefault()).toLocalDate().toString();
    }

    /**
     * A date in ISO-8601, keeping the time it carries and leaving out a midnight one.
     *
     * <p>The moment is written whole, down to the millisecond a date holds. A value rounded to the second would
     * read back as another moment, and a dimension date answers the requests its moment answers.
     */
    private static String iso(Date value) {
        var moment = LocalDateTime.ofInstant(value.toInstant(), ZoneId.systemDefault());
        return LocalTime.MIDNIGHT.equals(moment.toLocalTime())
                ? moment.toLocalDate().toString()
                : moment.toString();
    }
}
