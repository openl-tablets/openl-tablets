package org.openl;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;

import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.openl.message.Severity;
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

    private Collection<OpenLMessage> messages;

    private IOpenClass openClass;
    
    private ClassLoader classLoader;

    public CompiledOpenClass(IOpenClass openClass,
            Collection<OpenLMessage> messages,
            SyntaxNodeException[] parsingErrors,
            SyntaxNodeException[] bindingErrors) {
        
        this.openClass = openClass;
        this.parsingErrors = parsingErrors;
        this.bindingErrors = bindingErrors;
        if (messages == null) {
            this.messages = Collections.emptyList();
        } else {
            this.messages = new LinkedHashSet<>(messages);
        }
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
        Collection<OpenLMessage> errorMessages = OpenLMessagesUtils.filterMessagesBySeverity(getMessages(), Severity.ERROR);
        return (parsingErrors.length > 0) || (bindingErrors.length > 0) || 
            (errorMessages != null && !errorMessages.isEmpty());
    }

    public void throwErrorExceptionsIfAny() {
        Collection<OpenLMessage> errorMessages = OpenLMessagesUtils.filterMessagesBySeverity(getMessages(), Severity.ERROR);
        
        if (parsingErrors.length > 0) {
            throw new CompositeOpenlException("Parsing Error(s):", parsingErrors, errorMessages);
        }

        if (bindingErrors.length > 0) {
            throw new CompositeOpenlException("Binding Error(s):", bindingErrors, errorMessages);
        }
        
        if (!errorMessages.isEmpty()) {
        	throw new CompositeOpenlException("Module contains critical errors", null, errorMessages);
        }

    }

    public Collection<OpenLMessage> getMessages() {
        return Collections.unmodifiableCollection(messages);
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
