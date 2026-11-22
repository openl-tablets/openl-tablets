package org.openl.rules.table.properties;

import java.io.Serial;

import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.exception.SyntaxNodeException;

/**
 * As we have error gathering mechanism based on exceptions, this is a parent class for all error situations linked with
 * properties in all tables and in the property table component. If you need to describe new problem situations with
 * properties, extend your class from this one.
 *
 * @author DLiauchuk
 */
public class TablePropertiesException extends SyntaxNodeException {

    @Serial
    private static final long serialVersionUID = -6085682866325439833L;

    public TablePropertiesException(String message, Throwable cause, ISyntaxNode syntaxNode) {
        super(message, cause, syntaxNode);
    }

}
