package org.openl.rules.table.properties;

import org.openl.binding.IBoundNode;
import org.openl.binding.OpenLRuntimeException;

/**
 * As we have error gathering mechanism based on exceptions, this is a parent class for all error situations linked 
 * with properties in all tables and in the property table component. If you need to describe new problem situations 
 * with properties, extend your class from this one. 
 * 
 * @author DLiauchuk
 *
 */
public class TablePropertiesException extends OpenLRuntimeException {

    private static final long serialVersionUID = -6085682866325439833L;
    
    public TablePropertiesException(String message, IBoundNode node) {
        super(message, node);        
    }

    public TablePropertiesException(Throwable cause, IBoundNode node) {
        super(cause, node);        
    }
    
    public TablePropertiesException() {
        super();        
    }

    public TablePropertiesException(String arg0, Throwable arg1) {
        super(arg0, arg1);        
    }

    public TablePropertiesException(String arg0) {
        super(arg0);        
    }

    public TablePropertiesException(Throwable arg0) {
        super(arg0);        
    }
}
