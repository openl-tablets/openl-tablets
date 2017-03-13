/**
 * Created Jan 11, 2007
 */
package org.openl;

import java.util.List;
import java.util.Map;

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

    private List<OpenLMessage> messages;

    private IOpenClass openClass;
    
    private ClassLoader classLoader;

    public CompiledOpenClass(IOpenClass openClass,
            List<OpenLMessage> messages,
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
        List<OpenLMessage> errorMessages = getErrorMessages();
        return (parsingErrors.length > 0) || (bindingErrors.length > 0) || 
            (errorMessages != null && !errorMessages.isEmpty());
    }

    private List<OpenLMessage> getErrorMessages() {
        return OpenLMessagesUtils.filterMessagesBySeverity(getMessages(), Severity.ERROR);
    }

    public void throwErrorExceptionsIfAny() {
        if (parsingErrors.length > 0) {
            throw new CompositeOpenlException("Parsing Error(s):", parsingErrors, getErrorMessages());
        }

        if (bindingErrors.length > 0) {
            throw new CompositeOpenlException("Binding Error(s):", bindingErrors, getErrorMessages());
        }
        
        if (getErrorMessages().size() > 0) {
        	throw new CompositeOpenlException("Module contains critical errors", null, getErrorMessages());
        }

    }

    public List<OpenLMessage> getMessages() {
        return messages;
    }
    
    public Map<String, IOpenClass> getTypes() {
        if (openClass == null) {
            return null;
        }
        
        return openClass.getTypes();
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

}
