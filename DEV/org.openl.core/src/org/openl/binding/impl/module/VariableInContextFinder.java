package org.openl.binding.impl.module;

import org.openl.binding.exception.AmbiguousFieldException;
import org.openl.types.IOpenField;

public interface VariableInContextFinder {
    IOpenField findVariable(String fieldName, boolean strictMatch) throws AmbiguousFieldException;
}
