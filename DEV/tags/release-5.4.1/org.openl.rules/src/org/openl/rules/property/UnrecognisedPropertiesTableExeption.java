package org.openl.rules.property;

import org.openl.binding.IBoundNode;
import org.openl.rules.table.properties.TablePropertiesException;

/**
 * Exception class for situation when we have property table component but we can`t recognise is it 
 * module or category properties.
 * 
 * @author DLiauchuk
 *
 */
public class UnrecognisedPropertiesTableExeption extends TablePropertiesException {
    
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
