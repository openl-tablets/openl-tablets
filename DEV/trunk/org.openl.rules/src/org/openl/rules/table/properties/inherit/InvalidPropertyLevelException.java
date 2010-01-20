package org.openl.rules.table.properties.inherit;

/**
 * Handles situations when property is defined on wrong level. 
 * 
 * @author DLiauchuk
 *
 */
public class InvalidPropertyLevelException extends Exception {

    private static final long serialVersionUID = -5315833464199624657L;

    public InvalidPropertyLevelException() {
        // TODO Auto-generated constructor stub
    }

    public InvalidPropertyLevelException(String message) {
        super(message);
        // TODO Auto-generated constructor stub
    }

    public InvalidPropertyLevelException(Throwable cause) {
        super(cause);
        // TODO Auto-generated constructor stub
    }

    public InvalidPropertyLevelException(String message, Throwable cause) {
        super(message, cause);
        // TODO Auto-generated constructor stub
    }

}
