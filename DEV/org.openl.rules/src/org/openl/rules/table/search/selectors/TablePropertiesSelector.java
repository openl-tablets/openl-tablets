package org.openl.rules.table.search.selectors;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.time.DateUtils;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.util.AStringBoolOperator;
import org.openl.util.AStringBoolOperator.ContainsIgnoreCaseOperator;

/**
 * @author Andrei Astrouski
 */
public class TablePropertiesSelector extends TableSelector {

    private AStringBoolOperator stringMatchOperator;
    private Map<String, Object> properties;

    public TablePropertiesSelector() {
    }

    public TablePropertiesSelector(Map<String, Object> properties) {
        this(properties, new ContainsIgnoreCaseOperator(null));
    }

    public TablePropertiesSelector(Map<String, Object> properties, AStringBoolOperator stringMatchOperator) {
        this.properties = properties;
        this.stringMatchOperator = stringMatchOperator;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    public AStringBoolOperator getStringMatchOperator() {
        return stringMatchOperator;
    }

    public void setStringMatchOperator(AStringBoolOperator stringMatchOperator) {
        this.stringMatchOperator = stringMatchOperator;
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
            if (tableProperties != null){
                Object propValue = tableProperties.getPropertyValue(searchPropName);
                if (propValue != null && valuesEqual(searchPropValue, propValue)) {
                    numMatch++;
                }                
            }
        }

        return (numMatch == properties.size() && numMatch > 0);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private boolean valuesEqual(Object searchValue, Object value) {
        boolean result = false;

        if (value.getClass().equals(searchValue.getClass())) {
            if (value instanceof String) {
                result = stringMatchOperator.isMatching((String) searchValue, (String) value);

            } else if (value instanceof Date) {
                result = DateUtils.isSameDay((Date) searchValue, (Date) value);

            } else if (value instanceof Comparable<?>) {
                result = (((Comparable) value).compareTo(searchValue) == 0 ? true : false);

            } else if (value.getClass().isArray()) {
                List<Object> valueArray = Arrays.asList((Object[]) value);
                List<Object> searchValueArray = Arrays.asList((Object[]) searchValue);

                result = valueArray.containsAll(searchValueArray);
            }
        }

        return result;
    }

}
