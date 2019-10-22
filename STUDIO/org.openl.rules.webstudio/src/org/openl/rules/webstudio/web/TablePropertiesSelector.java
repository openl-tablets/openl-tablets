package org.openl.rules.webstudio.web;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.time.DateUtils;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.util.ASelector;
import org.openl.util.StringUtils;

/**
 * @author Andrei Astrouski
 */
class TablePropertiesSelector extends ASelector<TableSyntaxNode> {

    private final Map<String, Object> properties;

    public TablePropertiesSelector(Map<String, Object> properties) {
        this.properties = properties;
    }

    /**
     * Check if table properties consists all the values for properties from defined properties.
     */
    @Override
    public boolean select(TableSyntaxNode node) {
        ITableProperties tableProperties = node.getTableProperties();
        int numMatch = 0;

        for (Map.Entry<String, Object> searchProperty : properties.entrySet()) {
            String searchPropName = searchProperty.getKey();
            Object searchPropValue = searchProperty.getValue();
            if (tableProperties != null) {
                Object propValue = tableProperties.getPropertyValue(searchPropName);
                if (propValue != null && valuesEqual(searchPropValue, propValue)) {
                    numMatch++;
                }
            }
        }

        return numMatch == properties.size() && numMatch > 0;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private boolean valuesEqual(Object searchValue, Object value) {
        boolean result = false;

        if (value.getClass().equals(searchValue.getClass())) {
            if (value instanceof String) {
                result = StringUtils.containsIgnoreCase((String) value, (String) searchValue);

            } else if (value instanceof Date) {
                result = DateUtils.isSameDay((Date) searchValue, (Date) value);

            } else if (value instanceof Comparable<?>) {
                result = ((Comparable) value).compareTo(searchValue) == 0;

            } else if (value.getClass().isArray()) {
                List<Object> valueArray = Arrays.asList((Object[]) value);
                List<Object> searchValueArray = Arrays.asList((Object[]) searchValue);

                result = valueArray.containsAll(searchValueArray);
            }
        }

        return result;
    }

}
