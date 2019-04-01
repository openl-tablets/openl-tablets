package org.openl.rules.table.properties;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import org.openl.rules.method.ExecutableRulesMethod;
import org.openl.rules.table.properties.def.TablePropertyDefinitionUtils;
import org.openl.types.IOpenMethod;
import org.openl.types.impl.MethodKey;

/**
 * Immutable Key to check identity of {@link ExecutableRulesMethod} methods.
 *
 * Methods are identical when they have the same method signature and the same business dimension properties.
 *
 * @author DLiauchuk
 *
 */
public final class DimensionPropertiesMethodKey {

    private final IOpenMethod method;
    private int hashCode = 0;

    public DimensionPropertiesMethodKey(IOpenMethod method) {
        this.method = method;
    }

    public IOpenMethod getMethod() {
        return method;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof DimensionPropertiesMethodKey)) {
            return false;
        }
        DimensionPropertiesMethodKey key = (DimensionPropertiesMethodKey) obj;

        if (!new MethodKey(method).equals(new MethodKey(key.getMethod()))) {
            return false;
        }

        Map<String, Object> thisMethodProperties = PropertiesHelper.getTableProperties(method)
            .getAllDimensionalProperties();
        Map<String, Object> otherMethodProperties = PropertiesHelper.getTableProperties(key.getMethod())
            .getAllDimensionalProperties();

        if (thisMethodProperties.size() != otherMethodProperties.size()) {
            return false;
        }

        for (Entry<String, Object> entry : thisMethodProperties.entrySet()) {
            Object propertyValue1 = entry.getValue();
            Object propertyValue2 = otherMethodProperties.get(entry.getKey());

            if (isEmpty(propertyValue1) && isEmpty(propertyValue2)) {
                // There is no meaning in properties with "null" values.
                // If such properties exists, we should skip them like there is no empty properties.
                continue;
            }
            if (!Objects.deepEquals(propertyValue1, propertyValue2)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        if (hashCode == 0) {
            String[] dimensionalPropertyNames = TablePropertyDefinitionUtils.getDimensionalTablePropertiesNames();
            Map<String, Object> methodProperties = PropertiesHelper.getMethodProperties(method);
            int hash = new MethodKey(method).hashCode();
            if (methodProperties != null) {
                for (String dimensionalPropertyName : dimensionalPropertyNames) {
                    Object property = methodProperties.get(dimensionalPropertyName);
                    hash = 31 * hash + (property instanceof Object[] ? Arrays.deepHashCode((Object[]) property)
                                                                     : Objects.hashCode(property));
                }
            }
            hashCode = hash;
        }
        return hashCode;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(new MethodKey(method));
        String[] dimensionalPropertyNames = TablePropertyDefinitionUtils.getDimensionalTablePropertiesNames();

        stringBuilder.append('[');
        if (PropertiesHelper.getMethodProperties(method) != null) {
            for (int i = 0; i < dimensionalPropertyNames.length; i++) {
                if (i != 0) {
                    stringBuilder.append(',');
                }
                stringBuilder.append(dimensionalPropertyNames[i]).append('=');
                stringBuilder.append(
                    PropertiesHelper.getTableProperties(method).getPropertyValueAsString(dimensionalPropertyNames[i]));
            }
        }
        return stringBuilder.append(']').toString();
    }

    /**
     * Check if propertyValue is null or it contains only null values
     *
     * @param propertyValue checking value
     * @return true if propertyValue is null or it contains only null values. If it contains any not null value -
     *         falseT;
     */
    private boolean isEmpty(Object propertyValue) {
        if (propertyValue == null) {
            return true;
        }

        if (propertyValue.getClass().isArray()) {
            // Check if an array is empty or contains only nulls
            int length = Array.getLength(propertyValue);
            if (length == 0) {
                return true;
            }

            for (int i = 0; i < length; i++) {
                if (Array.get(propertyValue, i) != null) {
                    return false;
                }
            }

            return true;
        }

        return false;
    }

}
