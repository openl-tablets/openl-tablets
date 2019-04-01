package org.openl.rules.property.exception;

import org.openl.rules.table.properties.TablePropertiesException;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.ISyntaxNode;
import org.openl.util.text.ILocation;

/**
 * Exception class for situation when we have more than one module properties table or more than one table for category.
 *
 * @author DLiauchuk
 */
public class DuplicatedPropertiesTableException extends TablePropertiesException {

    private static final long serialVersionUID = -1637117550935829524L;

    public DuplicatedPropertiesTableException(String message,
            Throwable cause,
            ILocation location,
            IOpenSourceCodeModule source) {
        super(message, cause, location, source);
    }

    public DuplicatedPropertiesTableException(String message, Throwable cause, ISyntaxNode syntaxNode) {
        super(message, cause, syntaxNode);
    }

}
