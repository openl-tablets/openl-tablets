package org.openl.util.generation;

public class DefaultEmptyArrayConstructorInitWriter implements TypeInitializationWriter {
    @Override
    public String getInitialization(Object value) {
        if (value == null) {
            throw new IllegalArgumentException("Value cannot be null");
        }
        
        Class<?> type = value.getClass();
        if (!type.isArray()){
            throw new IllegalStateException("Requred array type of class!");
        }
        
        StringBuilder sb = new StringBuilder("new %s");
        while (type.isArray()){
            type = type.getComponentType();
            sb.append("[0]");
        }
        
        return String.format(sb.toString(), type.getCanonicalName());
    }
}
