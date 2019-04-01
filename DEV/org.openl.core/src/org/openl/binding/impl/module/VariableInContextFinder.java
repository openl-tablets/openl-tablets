package org.openl.binding.impl.module;

import org.openl.binding.exception.AmbiguousVarException;
import org.openl.types.IOpenField;

public interface VariableInContextFinder {
    IOpenField findVariable(String name) throws AmbiguousVarException;
}
