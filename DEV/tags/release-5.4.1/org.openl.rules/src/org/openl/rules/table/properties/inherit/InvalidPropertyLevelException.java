package org.openl.rules.table.properties.inherit;

import org.openl.rules.table.properties.TablePropertiesException;

/**
 * Occurs when property is defined on wrong level. For example if property is suitable just fior CATEGORY level and 
 * is defined on TABLE level, this is exception should be thrown.  
 * 
 * @author DLiauchuk
 *
 */
public class InvalidPropertyLevelException extends TablePropertiesException {

    private static final long serialVersionUID = -5315833464199624657L;

    public InvalidPropertyLevelException() {        
    }

    public InvalidPropertyLevelException(String message) {
        super(message);        
    }

    public InvalidPropertyLevelException(Throwable cause) {
        super(cause);        
    }

    public InvalidPropertyLevelException(String message, Throwable cause) {
        super(message, cause);        
    }

}
