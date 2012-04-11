package org.openl.rules.property;

import org.openl.binding.IBoundNode;
import org.openl.rules.table.properties.TablePropertiesException;

/**
 * Exception class for situation when we have more than one module properties table or more than 
 * one table for category.
 * 
 * @author DLiauchuk
 */
public class DuplicatedPropertiesTableException extends TablePropertiesException {

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
