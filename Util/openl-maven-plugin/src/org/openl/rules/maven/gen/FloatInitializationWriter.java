package org.openl.rules.maven.gen;

public class FloatInitializationWriter implements TypeInitializationWriter {

    public String getInitialization(Object value) {
        if (!(value instanceof Float)) {
            throw new IllegalStateException("Float arg is expected!");
        }
        
        return String.valueOf(value) + "f";
    }

}