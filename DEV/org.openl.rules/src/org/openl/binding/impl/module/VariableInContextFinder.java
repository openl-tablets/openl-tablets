package org.openl.binding.impl.module;

import org.jspecify.annotations.Nullable;

import org.openl.binding.exception.AmbiguousFieldException;
import org.openl.types.IOpenField;

public interface VariableInContextFinder {
    IOpenField findVariable(String fieldName, boolean strictMatch) throws AmbiguousFieldException;

    /**
     * Finds a field of the element of an array index operator.
     *
     * <p>The enclosing finder finds the variables of the context the operator is written in. When only some values of
     * the element type have the field, the finder can return a field that reads such a variable for an element
     * without it.
     *
     * <p>By default, the field of the element is found as {@link #findVariable} finds it.
     */
    default @Nullable IOpenField findElementField(String fieldName,
                                                 boolean strictMatch,
                                                 VariableInContextFinder enclosing) throws AmbiguousFieldException {
        return findVariable(fieldName, strictMatch);
    }
}
