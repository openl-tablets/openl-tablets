package org.openl.validation;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;

import org.openl.CompiledOpenClass;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.openl.message.Severity;
import org.openl.syntax.exception.CompositeOpenlException;
import org.openl.types.IOpenClass;

public class ValidatedCompiledOpenClass extends CompiledOpenClass {
    private final CompiledOpenClass delegate;
    private final Collection<OpenLMessage> additionalMessages = new LinkedHashSet<>();
    boolean hasErrors;

    public ValidatedCompiledOpenClass(CompiledOpenClass compiledOpenClass) {
        super(compiledOpenClass.getOpenClassWithErrors(), compiledOpenClass.getMessages());
        this.delegate = Objects.requireNonNull(compiledOpenClass, "compiledOpenClass cannot be null");
        this.hasErrors = compiledOpenClass.hasErrors();
    }

    @Override
    public IOpenClass getOpenClass() {
        return delegate.getOpenClass();
    }

    @Override
    public IOpenClass getOpenClassWithErrors() {
        return delegate.getOpenClassWithErrors();
    }

    @Override
    public boolean hasErrors() {
        return hasErrors;
    }

    @Override
    public void throwErrorExceptionsIfAny() {
        if (hasErrors()) {
            Collection<OpenLMessage> errorMessages = OpenLMessagesUtils.filterMessagesBySeverity(getMessages(),
                Severity.ERROR);
            throw new CompositeOpenlException("Module contains critical errors", null, errorMessages);
        }
    }

    @Override
    public Collection<OpenLMessage> getMessages() {
        Collection<OpenLMessage> messages = new LinkedHashSet<>(delegate.getMessages());
        messages.addAll(additionalMessages);
        return messages;
    }

    public void addMessage(OpenLMessage message) {
        this.additionalMessages.add(message);
        if (message.getSeverity() == Severity.ERROR) {
            hasErrors = true;
        }
    }

    @Override
    public Collection<IOpenClass> getTypes() {
        return delegate.getTypes();
    }

    @Override
    public ClassLoader getClassLoader() {
        return delegate.getClassLoader();
    }
}
