package org.openl.util.generation;

/**
 * The most common implementation. For initializing the income value 
 * returns its String representation by {@link String#valueOf(Object)} 
 * 
 * @author DLiauchuk
 *
 */
public class CommonInitializationWriter implements TypeInitializationWriter {

    public String getInitialization(Object value) {        
        return String.valueOf(value);
    }

}
