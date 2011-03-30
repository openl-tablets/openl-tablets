package org.openl.codegen.tools;

import org.apache.commons.beanutils.ConstructorUtils;
import org.openl.rules.helpers.NumberUtils;

/**
 * Class used by Velocity engine as external tools.
 */
public class VelocityTool {

    public int length(Object[] array) {
        return array.length;
    }
    
    public String getTypeName(Class<?> clazz) {
        
        if (clazz.isArray()) {
            return String.format("%s[]", clazz.getComponentType().getName());
        }
        
        return clazz.getName();
    }
    
    public boolean hasConstructorWithoutParams(Class<?> clazz) {
        if (ConstructorUtils.getAccessibleConstructor(clazz, new Class<?>[] {}) != null) {
            return true;
        }
        return false;
    }

    public boolean hasConstructorWithPropertyName(Class<?> clazz) {
        if (ConstructorUtils.getAccessibleConstructor(clazz, new Class<?>[] { String.class }) != null) {
            return true;
        }
        return false;
    }

    public boolean hasConstructorWithConstraintForProperty(Class<?> clazz) {
        if (ConstructorUtils.getAccessibleConstructor(clazz, new Class<?>[] { String.class, String.class }) != null) {
            return true;
        }
        return false;
    }
    
    public String formatAccessorName(String name) {
        
        StringBuilder builder = new StringBuilder();
        
        builder.append(name.substring(0,1).toUpperCase())
            .append(name.substring(1));
        
        return builder.toString();
    }
    
    public boolean isNotPrimitiveDouble(Class<?> clazz) {
        return !double.class.equals(clazz);
    }
    
    public static Class<?> getNumericPrimitive(Class<?> wrapperClass) {
        return NumberUtils.getNumericPrimitive(wrapperClass);
    }
}
