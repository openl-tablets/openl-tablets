package org.openl.rules.lang.xls.prebind;

import org.openl.types.IOpenMember;

public interface ILazyMember<T extends IOpenMember> {
    
    T getOriginal();
    
}
