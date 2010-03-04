package org.openl.rules.property;

import org.openl.binding.IBoundNode;
import org.openl.binding.OpenLRuntimeException;

public class UnrecognisedPropertiesTableExeption extends OpenLRuntimeException {
    
    private static final long serialVersionUID = -4605677258951761707L;

    public UnrecognisedPropertiesTableExeption() {
        super();
    }

    public UnrecognisedPropertiesTableExeption(String message, Throwable cause) {
        super(message, cause);        
    }

    public UnrecognisedPropertiesTableExeption(String message) {
        super(message);        
    }

    public UnrecognisedPropertiesTableExeption(Throwable cause) {
        super(cause);        
    }

    public UnrecognisedPropertiesTableExeption(Throwable cause, IBoundNode node) {
        super(cause, node);
    }

    public UnrecognisedPropertiesTableExeption(String message, IBoundNode node) {
        super(message, node);        
    }

}
