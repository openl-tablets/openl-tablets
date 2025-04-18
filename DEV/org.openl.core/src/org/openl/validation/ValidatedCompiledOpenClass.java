package org.openl.validation;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;

import org.openl.CompiledOpenClass;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.openl.message.Severity;
import org.openl.syntax.exception.CompositeOpenlException;
import org.openl.types.IOpenClass;

public final class ValidatedCompiledOpenClass extends CompiledOpenClass {
    private final CompiledOpenClass delegate;
    private final Collection<OpenLMessage> validationMessages = new LinkedHashSet<>();
    private final boolean hasCompilationErrors;
    private boolean hasValidationErrors;

    public static ValidatedCompiledOpenClass instanceOf(CompiledOpenClass compiledOpenClass) {
        if (compiledOpenClass instanceof ValidatedCompiledOpenClass) {
            return (ValidatedCompiledOpenClass) compiledOpenClass;
        } else {
            return new ValidatedCompiledOpenClass(compiledOpenClass);
        }
    }

    private ValidatedCompiledOpenClass(CompiledOpenClass compiledOpenClass) {
        super(compiledOpenClass.getOpenClassWithErrors(), compiledOpenClass.getAllMessages());
        this.delegate = Objects.requireNonNull(compiledOpenClass, "compiledOpenClass cannot be null");
        this.hasCompilationErrors = compiledOpenClass.hasErrors();
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
        return hasCompilationErrors || hasValidationErrors;
    }

    @Override
    public void throwErrorExceptionsIfAny() {
        if (hasErrors()) {
            Collection<OpenLMessage> errorMessages = OpenLMessagesUtils.filterMessagesBySeverity(getAllMessages(),
                    Severity.ERROR);
            throw new CompositeOpenlException("Module contains critical errors", null, errorMessages);
        }
    }

    @Override
    public Collection<OpenLMessage> getAllMessages() {
        Collection<OpenLMessage> messages = new LinkedHashSet<>(delegate.getAllMessages());
        messages.addAll(validationMessages);
        return messages;
    }

    public Collection<OpenLMessage> getValidationMessages() {
        return Collections.unmodifiableCollection(validationMessages);
    }

    public void addMessage(OpenLMessage message) {
        this.validationMessages.add(message);
        if (message.getSeverity() == Severity.ERROR) {
            hasValidationErrors = true;
        }
    }

    public boolean hasOnlyValidationErrors() {
        return !hasCompilationErrors && hasValidationErrors;
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
