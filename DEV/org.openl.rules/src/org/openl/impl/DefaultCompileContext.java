package org.openl.impl;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.openl.ICompileContext;
import org.openl.validation.IOpenLValidator;

/**
 * Default implementation of the {@link ICompileContext} interface.
 */
public class DefaultCompileContext implements ICompileContext {

    /**
     * Set of validators that will be used in validation process.
     */
    private final Set<IOpenLValidator> validators = new CopyOnWriteArraySet<>();

    /**
     * {@inheritDoc}
     */
    @Override
    public void addValidator(IOpenLValidator validator) {
        validators.add(validator);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<IOpenLValidator> getValidators() {
        return validators;
    }
}
