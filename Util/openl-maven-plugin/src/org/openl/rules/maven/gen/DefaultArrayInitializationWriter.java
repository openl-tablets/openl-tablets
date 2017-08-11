package org.openl.rules.maven.gen;

public class DefaultArrayInitializationWriter implements TypeInitializationWriter {
    
    @Override
    public String getInitialization(Object value) {
        if (value == null) {
            throw new IllegalArgumentException("Value can't be null");
        }
        
        Class<?> type = value.getClass();
        if (!type.isArray()){
            throw new IllegalStateException("Array type is expected!");
        }
        
        StringBuilder sb = new StringBuilder("new %s");
        while (type.isArray()){
            type = type.getComponentType();
            sb.append("[0]");
        }
        
        return String.format(sb.toString(), type.getSimpleName());
    }
}
