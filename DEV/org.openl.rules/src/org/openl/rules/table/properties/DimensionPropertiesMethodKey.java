package org.openl.rules.table.properties;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.openl.rules.method.ExecutableRulesMethod;
import org.openl.rules.table.properties.def.TablePropertyDefinitionUtils;
import org.openl.types.IOpenMethod;
import org.openl.types.impl.MethodKey;

import java.lang.reflect.Array;
import java.util.Map;

/**
 * Immutable Key to check identity of {@link ExecutableRulesMethod} methods.
 *
 * Methods are identical when they have the same method signature and the same business
 * dimension properties. 
 * 
 * @author DLiauchuk
 *
 */
public final class DimensionPropertiesMethodKey {
    
    private final IOpenMethod method;
    
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

        // TODO: remove usage of the EqualsBuilder
        //
        EqualsBuilder equalsBuilder = new EqualsBuilder();
        equalsBuilder.append(new MethodKey(method), new MethodKey(key.getMethod()));
        String[] dimensionalPropertyNames = TablePropertyDefinitionUtils.getDimensionalTablePropertiesNames();
        for (int i = 0; i < dimensionalPropertyNames.length; i++) {
            Object propertyValue1 = PropertiesHelper.getMethodProperties(method).get(
                    dimensionalPropertyNames[i]);
            Object propertyValue2 = PropertiesHelper.getMethodProperties(key.getMethod()).get(
                    dimensionalPropertyNames[i]);
            
            if (isEmpty(propertyValue1) && isEmpty(propertyValue2)) {
                // There is no meaning in properties with "null" values.
                // If such properties exists, we should skip them like there is no empty properties.
                continue;
            }
            
            equalsBuilder.append(propertyValue1, propertyValue2);
        }
        return equalsBuilder.isEquals();
    }

    @Override
    public int hashCode() {
        String[] dimensionalPropertyNames = TablePropertyDefinitionUtils.getDimensionalTablePropertiesNames();
        Map<String, Object> methodProperties = PropertiesHelper.getMethodProperties(method);
        HashCodeBuilder hashCodeBuilder = new HashCodeBuilder();
        hashCodeBuilder.append(new MethodKey(method));
        if (methodProperties != null) {
            for (String dimensionalPropertyName : dimensionalPropertyNames) {
                Object property = methodProperties.get(dimensionalPropertyName);
                hashCodeBuilder.append(property);
            }
        }
        return hashCodeBuilder.build();
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
                stringBuilder.append(PropertiesHelper.getTableProperties(method)
                    .getPropertyValueAsString(dimensionalPropertyNames[i]));
            }
        }        
        return stringBuilder.append(']').toString();
    }
    
    
    /**
     * Check if propertyValue is null or it contains only null values
     * 
     * @param propertyValue checking value
     * @return true if propertyValue is null or it contains only null values. If it contains any not null value - false; 
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
