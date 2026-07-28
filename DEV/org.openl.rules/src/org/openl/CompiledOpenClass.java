package org.openl;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import lombok.Getter;

import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.openl.message.Severity;
import org.openl.syntax.exception.CompositeOpenlException;
import org.openl.types.IOpenClass;

/**
 * Handles <code>{@link IOpenClass}</code> with parsing and compiled errors.
 *
 * @author snshor
 */
public class CompiledOpenClass {

    @Getter
    private final Collection<OpenLMessage> allMessages;

    @Getter
    private final Collection<OpenLMessage> messages;

    private final IOpenClass openClass;

    private boolean hasErrors;

    @Getter
    private final ClassLoader classLoader;

    public CompiledOpenClass(IOpenClass openClass, Collection<OpenLMessage> messages) {
        this(openClass, messages, null);
    }

    public CompiledOpenClass(IOpenClass openClass,
                             Collection<OpenLMessage> allMessages,
                             Collection<OpenLMessage> messages) {
        this.messages = messages != null ? Collections.unmodifiableCollection(messages) : List.of();
        this.openClass = Objects.requireNonNull(openClass, "openClass cannot be null");
        if (allMessages == null) {
            this.allMessages = List.of();
        } else {
            this.allMessages = Collections.unmodifiableCollection(allMessages);
            this.hasErrors = !OpenLMessagesUtils.filterMessagesBySeverity(allMessages, Severity.ERROR).isEmpty();
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
            var errorMessages = OpenLMessagesUtils.filterMessagesBySeverity(allMessages,
                    Severity.ERROR);
            throw new CompositeOpenlException("Module contains critical errors", null, errorMessages);
        }
    }

    public Collection<IOpenClass> getTypes() {
        return openClass != null ? openClass.getTypes() : null;
    }

}
