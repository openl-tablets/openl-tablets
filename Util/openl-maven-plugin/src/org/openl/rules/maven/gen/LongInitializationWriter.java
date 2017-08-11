package org.openl.rules.maven.gen;

public class LongInitializationWriter implements TypeInitializationWriter {

    public String getInitialization(Object value) {
        if (!(value instanceof Long)) {
            throw new IllegalStateException("Long arg is expected!");
        }
        
        return String.valueOf(value) + "l";
    }

}