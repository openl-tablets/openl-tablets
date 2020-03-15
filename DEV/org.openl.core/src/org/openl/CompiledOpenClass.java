package org.openl;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.openl.message.Severity;
import org.openl.syntax.exception.CompositeOpenlException;
import org.openl.types.IOpenClass;

/**
 * Handles <code>{@link IOpenClass}</code> with parsing and compiled errors.
 *
 * @author snshor
 *
 */
public class CompiledOpenClass {

    private Collection<OpenLMessage> messages;

    private IOpenClass openClass;

    private boolean hasErrors;

    private ClassLoader classLoader;

    public CompiledOpenClass(IOpenClass openClass, Collection<OpenLMessage> messages) {
        this.openClass = Objects.requireNonNull(openClass, "openClass cannot be null");
        if (messages == null) {
            this.messages = Collections.emptyList();
        } else {
            this.messages = Collections.unmodifiableCollection(messages);
            this.hasErrors = !OpenLMessagesUtils.filterMessagesBySeverity(messages, Severity.ERROR).isEmpty();
        }
        this.classLoader = Thread.currentThread().getContextClassLoader();
    }

    public IOpenClass getOpenClass() {
        throwErrorExceptionsIfAny();
        return openClass;
    }

    public IOpenClass getOpenClassWithErrors() {
        return openClass;
    }

    public boolean hasErrors() {
        return hasErrors;
    }

    public void throwErrorExceptionsIfAny() {
        if (hasErrors()) {
            Collection<OpenLMessage> errorMessages = OpenLMessagesUtils.filterMessagesBySeverity(messages,
                Severity.ERROR);
            throw new CompositeOpenlException("Module contains critical errors", null, errorMessages);
        }
    }

    public Collection<OpenLMessage> getMessages() {
        return messages;
    }

    public Collection<IOpenClass> getTypes() {
        return openClass != null ? openClass.getTypes() : null;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

}
