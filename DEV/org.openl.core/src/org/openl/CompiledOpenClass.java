package org.openl;

import java.util.Collection;

import org.openl.message.IOpenLMessages;
import org.openl.message.OpenLMessage;
import org.openl.syntax.exception.CompositeOpenlException;
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

    private IOpenLMessages messages;

    private IOpenClass openClass;
    
    private ClassLoader classLoader;

    public CompiledOpenClass(IOpenClass openClass,
            IOpenLMessages messages,
            SyntaxNodeException[] parsingErrors,
            SyntaxNodeException[] bindingErrors) {
        
        this.openClass = openClass;
        this.parsingErrors = parsingErrors;
        this.bindingErrors = bindingErrors;
        this.messages = messages;
        this.classLoader = Thread.currentThread().getContextClassLoader();
    }

    @Deprecated
    public SyntaxNodeException[] getBindingErrors() { 
        return bindingErrors;
    }

    public IOpenClass getOpenClass() {
        throwErrorExceptionsIfAny();
        return openClass;
    }

    public IOpenClass getOpenClassWithErrors() {
        return openClass;
    }

    @Deprecated
    public SyntaxNodeException[] getParsingErrors() {
        return parsingErrors;
    }

    public boolean hasErrors() {
        Collection<OpenLMessage> errorMessages = getOpenLMessages().getErrors();
        return (parsingErrors.length > 0) || (bindingErrors.length > 0) || 
            (errorMessages != null && !errorMessages.isEmpty());
    }

    public void throwErrorExceptionsIfAny() {
        if (parsingErrors.length > 0) {
            throw new CompositeOpenlException("Parsing Error(s):", parsingErrors, getOpenLMessages().getErrors());
        }

        if (bindingErrors.length > 0) {
            throw new CompositeOpenlException("Binding Error(s):", bindingErrors, getOpenLMessages().getErrors());
        }
        
        if (getOpenLMessages().hasErrors()) {
        	throw new CompositeOpenlException("Module contains critical errors", null, getOpenLMessages().getErrors());
        }

    }

    public IOpenLMessages getOpenLMessages() {
        return messages;
    }
    
    public Collection<IOpenClass> getTypes() {
        if (openClass == null) {
            return null;
        }
        
        return openClass.getTypes();
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

}
