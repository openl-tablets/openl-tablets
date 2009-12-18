/**
 * 
 */
package org.openl.rules.binding;

import org.openl.binding.IBoundNode;
import org.openl.binding.OpenLRuntimeException;

/**
 * @author DLiauchuk
 * 
 */
public class DublicatedPropertiesTableException extends OpenLRuntimeException {

    private static final long serialVersionUID = -1637117550935829524L;

    public DublicatedPropertiesTableException() {
        super();
    }

    public DublicatedPropertiesTableException(String message, Throwable cause) {
        super(message, cause);
    }

    public DublicatedPropertiesTableException(String message) {
        super(message);
    }

    public DublicatedPropertiesTableException(Throwable cause) {
        super(cause);
    }

    public DublicatedPropertiesTableException(Throwable cause, IBoundNode node) {
        super(cause, node);
    }
    
    public DublicatedPropertiesTableException(String message, IBoundNode node) {
        super(message, node);
    }

}
