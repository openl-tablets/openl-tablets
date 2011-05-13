package org.openl.util.generation;

public class CommonInitializationWriter implements TypeInitializationWriter {

    public String getInitialization(Object value) {        
        return String.valueOf(value);
    }

}
