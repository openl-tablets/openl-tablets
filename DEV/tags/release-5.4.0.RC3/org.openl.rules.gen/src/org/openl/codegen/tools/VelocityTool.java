package org.openl.codegen.tools;

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
    
    public String formatAccessorName(String name) {
        
        StringBuilder builder = new StringBuilder();
        
        builder.append(name.substring(0,1).toUpperCase())
            .append(name.substring(1));
        
        return builder.toString();
    }
}
