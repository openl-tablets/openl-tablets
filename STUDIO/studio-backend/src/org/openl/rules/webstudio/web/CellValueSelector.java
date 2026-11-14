package org.openl.rules.webstudio.web;

import java.lang.reflect.Array;
import java.util.Date;
import java.util.function.Predicate;

import org.apache.commons.lang3.time.DateUtils;
import org.jspecify.annotations.Nullable;

import org.openl.rules.convertor.String2DataConvertorFactory;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.util.StringUtils;

/**
 * Selects the tables that have a cell holding the searched text.
 *
 * <p>A cell matches when its value contains the text, ignoring the case. A date cell also matches when the text is a
 * date of the same day, written as a date is written in a table, such as {@code 3/12/2014} or {@code 2014-03-12}.
 */
public class CellValueSelector implements Predicate<TableSyntaxNode> {

    private final String value;
    private final @Nullable Date date;

    public CellValueSelector(String value) {
        this.value = value;
        this.date = dateOf(value);
    }

    private static @Nullable Date dateOf(String text) {
        try {
            return String2DataConvertorFactory.parse(Date.class, text.strip(), null);
        } catch (RuntimeException e) {
            return null;
        }
    }

    @Override
    public boolean test(TableSyntaxNode node) {
        if (StringUtils.isBlank(value)) {
            return true;
        }
        var table = node.getGridTable();
        for (var row = 0; row < table.getHeight(); row++) {
            for (var col = 0; col < table.getWidth(); col++) {
                var cell = table.getCell(col, row);
                var cellValue = cell.getObjectValue();
                if (selectValue(cellValue)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean selectValue(Object cellValue) {
        if (cellValue == null) {
            return false;
        }

        if (cellValue.getClass().isArray()) {
            var len = Array.getLength(cellValue);
            for (var i = 0; i < len; i++) {
                Object cv = Array.get(cellValue, i);
                if (selectValue(cv)) {
                    return true;
                }
            }
            return false;
        }

        if (date != null && cellValue instanceof Date cellDate && DateUtils.isSameDay(date, cellDate)) {
            return true;
        }

        return StringUtils.containsIgnoreCase(String.valueOf(cellValue), value);
    }

}
