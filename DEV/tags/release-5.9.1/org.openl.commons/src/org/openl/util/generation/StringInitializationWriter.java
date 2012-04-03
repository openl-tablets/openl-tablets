package org.openl.util.generation;

public class StringInitializationWriter extends CommonInitializationWriter {
    
    @Override
    public String getInitialization(Object value) {        
        return String.format("\"%s\"", super.getInitialization(value));
    }
}
