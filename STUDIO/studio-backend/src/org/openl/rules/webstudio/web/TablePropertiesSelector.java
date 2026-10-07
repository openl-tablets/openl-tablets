package org.openl.rules.webstudio.web;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
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
     * Check if table properties consists all the values for properties from defined properties.
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

            } else if (value.getClass().isArray()) {
                List<Object> valueArray = Arrays.asList((Object[]) value);
                List<Object> searchValueArray = Arrays.asList((Object[]) searchValue);

                result = valueArray.containsAll(searchValueArray);
            }
        }

        return result;
    }

}
