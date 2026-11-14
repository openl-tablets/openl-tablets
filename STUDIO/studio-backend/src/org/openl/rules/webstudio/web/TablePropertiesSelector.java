package org.openl.rules.webstudio.web;

import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.function.Predicate;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.time.DateUtils;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.util.StringUtils;

/**
 * @author Andrei Astrouski
 */
@RequiredArgsConstructor
public class TablePropertiesSelector implements Predicate<TableSyntaxNode> {

    private final Map<String, Object> properties;

    /**
     * Checks that the table declares every searched property with a matching value.
     *
     * <p>A text value matches when the table value contains it, ignoring the case. A date matches the same day. A list
     * of values, such as several tags, matches when the table declares at least one of them.
     */
    @Override
    public boolean test(TableSyntaxNode node) {
        var tableProperties = node.getTableProperties();
        var numMatch = 0;

        for (Map.Entry<String, Object> searchProperty : properties.entrySet()) {
            var searchPropName = searchProperty.getKey();
            var searchPropValue = searchProperty.getValue();
            if (tableProperties != null) {
                var propValue = tableProperties.getPropertyValue(searchPropName);
                if (propValue != null && valuesEqual(searchPropValue, propValue)) {
                    numMatch++;
                }
            }
        }

        return numMatch == properties.size() && numMatch > 0;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private boolean valuesEqual(Object searchValue, Object value) {
        var result = false;

        if (value.getClass().equals(searchValue.getClass())) {
            if (value instanceof String string) {
                result = StringUtils.containsIgnoreCase(string, (String) searchValue);

            } else if (value instanceof Date date) {
                result = DateUtils.isSameDay((Date) searchValue, date);

            } else if (value instanceof Comparable comparable) {
                result = comparable.compareTo(searchValue) == 0;

            } else if (value instanceof Object[] values) {
                var declared = Arrays.asList(values);
                result = Arrays.stream((Object[]) searchValue).anyMatch(declared::contains);
            }
        }

        return result;
    }

}
