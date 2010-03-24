/**
 * Created Jan 11, 2007
 */
package org.openl;

import org.openl.syntax.error.ISyntaxNodeError;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.types.IOpenClass;

/**
 * Handles <code>{@link IOpenClass}</code> with parsing and compiled errors.
 * 
 * @author snshor
 *
 */
public class CompiledOpenClass {

    private ISyntaxNodeError[] parsingErrors;

    private ISyntaxNodeError[] bindingErrors;

    private IOpenClass openClass;

    public CompiledOpenClass(IOpenClass openClass, ISyntaxNodeError[] parsingErrors, ISyntaxNodeError[] bindingErrors) {
        this.openClass = openClass;
        this.parsingErrors = parsingErrors;
        this.bindingErrors = bindingErrors;
    }

    public ISyntaxNodeError[] getBindingErrors() {
        return bindingErrors;
    }

    public IOpenClass getOpenClass() {
        throwErrorExceptionsIfAny();
        return openClass;
    }

    /**
     * @return
     */
    public IOpenClass getOpenClassWithErrors() {
        return openClass;
    }

    public ISyntaxNodeError[] getParsingErrors() {
        return parsingErrors;
    }

    public boolean hasErrors() {
        return (parsingErrors.length > 0) || (bindingErrors.length > 0);
    }

    public void throwErrorExceptionsIfAny() {
        if (parsingErrors.length > 0) {
            throw new SyntaxNodeException("Parsing Error(s):", parsingErrors);
        }

        if (bindingErrors.length > 0) {
            throw new SyntaxNodeException("Binding Error(s):", bindingErrors);
        }

    }

}
