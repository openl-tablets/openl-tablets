/**
 * 
 */
package org.openl.rules.property;

import org.openl.binding.IBoundNode;
import org.openl.binding.OpenLRuntimeException;

/**
 * @author DLiauchuk
 * 
 */
public class DuplicatedPropertiesTableException extends OpenLRuntimeException {

    private static final long serialVersionUID = -1637117550935829524L;

    public DuplicatedPropertiesTableException() {
        super();
    }

    public DuplicatedPropertiesTableException(String message, Throwable cause) {
        super(message, cause);
    }

    public DuplicatedPropertiesTableException(String message) {
        super(message);
    }

    public DuplicatedPropertiesTableException(Throwable cause) {
        super(cause);
    }

    public DuplicatedPropertiesTableException(Throwable cause, IBoundNode node) {
        super(cause, node);
    }
    
    public DuplicatedPropertiesTableException(String message, IBoundNode node) {
        super(message, node);
    }

}
