package org.openl.util.generation;

public class CharInitializationWriter extends CommonInitializationWriter {
    
    @Override
    public String getInitialization(Object value) {        
        return String.format("'%s'", super.getInitialization(value));
    }
}
