package org.openl.rules.table.properties;

import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.util.text.ILocation;

/**
 * As we have error gathering mechanism based on exceptions, this is a parent class for all error situations linked with
 * properties in all tables and in the property table component. If you need to describe new problem situations with
 * properties, extend your class from this one.
 * 
 * @author DLiauchuk
 *
 */
public class TablePropertiesException extends SyntaxNodeException {

    private static final long serialVersionUID = -6085682866325439833L;

    public TablePropertiesException(String message, Throwable cause, ILocation location, IOpenSourceCodeModule source) {
        super(message, cause, location, source);
    }

    public TablePropertiesException(String message, Throwable cause, ISyntaxNode syntaxNode) {
        super(message, cause, syntaxNode);
    }

}
