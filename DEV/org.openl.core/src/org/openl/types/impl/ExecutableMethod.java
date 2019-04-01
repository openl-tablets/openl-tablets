package org.openl.types.impl;

import org.openl.types.IMemberMetaInfo;
import org.openl.types.IOpenMethodHeader;

/**
 * Default implementation for all executable OpenL methods.
 *
 * @author DLiauchuk
 *
 */
public abstract class ExecutableMethod extends AMethod implements IMemberMetaInfo {

    public ExecutableMethod(IOpenMethodHeader header) {
        super(header);
    }

    @Override
    public boolean isConstructor() {
        return false;
    }

    @Override
    public String toString() {
        return getName();
    }
}
