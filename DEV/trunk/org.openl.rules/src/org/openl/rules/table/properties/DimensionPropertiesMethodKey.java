package org.openl.rules.table.properties;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.openl.rules.ExecutableRulesMethod;
import org.openl.rules.table.properties.def.TablePropertyDefinitionUtils;
import org.openl.types.impl.MethodKey;

/**
 * Key to check identity of {@link ExecutableRulesMethod} methods. Methods are
 * identical when they have the same method signature and the same business
 * dimension properties. 
 * 
 * @author DLiauchuk
 *
 */
public class DimensionPropertiesMethodKey {
    
    private ExecutableRulesMethod executableMethod;
    
    public DimensionPropertiesMethodKey(ExecutableRulesMethod method) {
        this.executableMethod = method;
    }

    public ExecutableRulesMethod getMethod() {
        return executableMethod;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof DimensionPropertiesMethodKey)) {
            return false;
        }
        DimensionPropertiesMethodKey key = (DimensionPropertiesMethodKey) obj;

        EqualsBuilder equalsBuilder = new EqualsBuilder();
        equalsBuilder.append(new MethodKey(executableMethod), new MethodKey(key.getMethod()));
        String[] dimensionalPropertyNames = TablePropertyDefinitionUtils.getDimensionalTablePropertiesNames();
        for (int i = 0; i < dimensionalPropertyNames.length; i++) {
            if (executableMethod.getMethodProperties() != null && key.getMethod().getMethodProperties() != null) {
                equalsBuilder.append(executableMethod.getProperties().get(dimensionalPropertyNames[i]), 
                    key.getMethod().getProperties().get(dimensionalPropertyNames[i]));
            }
        }
        return equalsBuilder.isEquals();
    }

    @Override
    public int hashCode() {
        HashCodeBuilder hashCodeBuilder = new HashCodeBuilder();
        hashCodeBuilder.append(new MethodKey(executableMethod));
        String[] dimensionalPropertyNames = TablePropertyDefinitionUtils.getDimensionalTablePropertiesNames();
        for (int i = 0; i < dimensionalPropertyNames.length; i++) {
            if (executableMethod.getMethodProperties() != null) {
                hashCodeBuilder.append(executableMethod.getProperties().get(dimensionalPropertyNames[i]));
            }
        }
        return hashCodeBuilder.toHashCode();
    }
    
    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(new MethodKey(executableMethod));
        String[] dimensionalPropertyNames = TablePropertyDefinitionUtils.getDimensionalTablePropertiesNames();
        
        stringBuilder.append('[');
        if (executableMethod.getMethodProperties() != null) {
            for (int i = 0; i < dimensionalPropertyNames.length; i++) {
                if (i != 0) {
                    stringBuilder.append(',');
                }
                stringBuilder.append(dimensionalPropertyNames[i]).append('=');
                stringBuilder.append(executableMethod.getMethodProperties().
                    getPropertyValueAsString(dimensionalPropertyNames[i]));
            }
        }        
        return stringBuilder.append(']').toString();
    }

}
