/**
 * Created Jan 11, 2007
 */
package org.openl;

import org.openl.syntax.ISyntaxError;
import org.openl.syntax.SyntaxErrorException;
import org.openl.types.IOpenClass;

/**
 * Handles <code>{@link IOpenClass}</code> with parsing and compiled errors.
 * 
 * @author snshor
 *
 */
public class CompiledOpenClass {

    ISyntaxError[] parsingErrors;

    ISyntaxError[] bindingErrors;

    IOpenClass openClass;

    public CompiledOpenClass(IOpenClass openClass, ISyntaxError[] parsingErrors, ISyntaxError[] bindingErrors) {
        this.openClass = openClass;
        this.parsingErrors = parsingErrors;
        this.bindingErrors = bindingErrors;
    }

    public ISyntaxError[] getBindingErrors() {
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

    public ISyntaxError[] getParsingErrors() {
        return parsingErrors;
    }

    public boolean hasErrors() {
        return (parsingErrors.length > 0) || (bindingErrors.length > 0);
    }

    public void throwErrorExceptionsIfAny() {
        if (parsingErrors.length > 0) {
            throw new SyntaxErrorException("Parsing Error(s):", parsingErrors);
        }

        if (bindingErrors.length > 0) {
            throw new SyntaxErrorException("Binding Error(s):", bindingErrors);
        }

    }

}
