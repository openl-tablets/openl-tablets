package org.openl.rules.ruleservice.core.interceptors;

import org.openl.types.IOpenMember;

/**
 * This interface is designed to inject @{@link IOpenMember} related to invoked rule method to ruleservice interceptors.
 */
@Deprecated
public interface IOpenMemberAware {
    void setIOpenMember(IOpenMember openMember);
}
