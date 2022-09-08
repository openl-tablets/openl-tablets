package org.openl.rules.ruleservice.core.interceptors;

import org.openl.types.IOpenClass;

/**
 * This interface is designed to inject @{@link IOpenClass} related to compiled project to ruleservice interceptors.
 */
public interface IOpenClassAware {
    void setIOpenClass(IOpenClass openClass);
}
