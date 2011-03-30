package org.openl.codegen.tools;

import org.openl.rules.helpers.NumberUtils;
import org.openl.rules.validation.ActivePropertyValidator;

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
        
        if (clazz.equals(ActivePropertyValidator.class)) {
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
