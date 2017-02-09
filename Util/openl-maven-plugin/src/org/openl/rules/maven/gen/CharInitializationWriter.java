package org.openl.rules.maven.gen;

/**
 * Initialization writer for values that are of type char or Character.
 * 
 * @author DLiauchuk
 * 
 */
public class CharInitializationWriter extends CommonInitializationWriter {

    @Override
    public String getInitialization(Object value) {
        return String.format("'%s'", super.getInitialization(value));
    }
}
