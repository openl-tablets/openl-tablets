/**
 * Created Jan 11, 2007
 */
package org.openl;

import org.openl.syntax.exception.CompositeSyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.types.IOpenClass;

/**
 * Handles <code>{@link IOpenClass}</code> with parsing and compiled errors.
 * 
 * @author snshor
 *
 */
public class CompiledOpenClass {

    private SyntaxNodeException[] parsingErrors;

    private SyntaxNodeException[] bindingErrors;

    private IOpenClass openClass;

    public CompiledOpenClass(IOpenClass openClass, SyntaxNodeException[] parsingErrors, SyntaxNodeException[] bindingErrors) {
        this.openClass = openClass;
        this.parsingErrors = parsingErrors;
        this.bindingErrors = bindingErrors;
    }

    public SyntaxNodeException[] getBindingErrors() {
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

    public SyntaxNodeException[] getParsingErrors() {
        return parsingErrors;
    }

    public boolean hasErrors() {
        return (parsingErrors.length > 0) || (bindingErrors.length > 0);
    }

    public void throwErrorExceptionsIfAny() {
        if (parsingErrors.length > 0) {
            throw new CompositeSyntaxNodeException("Parsing Error(s):", parsingErrors);
        }

        if (bindingErrors.length > 0) {
            throw new CompositeSyntaxNodeException("Binding Error(s):", bindingErrors);
        }

    }

}
