package org.openl.rules.table.properties;

import org.openl.binding.IBoundNode;
import org.openl.binding.OpenLRuntimeException;

public class TablePropertiesException extends OpenLRuntimeException {

    private static final long serialVersionUID = -6085682866325439833L;
    
    /**
     * @param message
     * @param node
     */
    public TablePropertiesException(String message, IBoundNode node) {
        super(message, node);
        // TODO Auto-generated constructor stub
    }

    /**
     * @param cause
     * @param node
     */
    public TablePropertiesException(Throwable cause, IBoundNode node) {
        super(cause, node);
        // TODO Auto-generated constructor stub
    }
    
    /**
     * 
     */
    public TablePropertiesException() {
        super();
        // TODO Auto-generated constructor stub
    }

    /**
     * @param arg0
     * @param arg1
     */
    public TablePropertiesException(String arg0, Throwable arg1) {
        super(arg0, arg1);
        // TODO Auto-generated constructor stub
    }

    /**
     * @param arg0
     */
    public TablePropertiesException(String arg0) {
        super(arg0);
        // TODO Auto-generated constructor stub
    }

    /**
     * @param arg0
     */
    public TablePropertiesException(Throwable arg0) {
        super(arg0);
        // TODO Auto-generated constructor stub
    }
}
