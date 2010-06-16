/*
 * Created on May 9, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.types;

import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 *
 */
public interface IOpenField extends IOpenMember {
    Object get(Object target, IRuntimeEnv env);

    void set(Object target, Object value, IRuntimeEnv env);

    boolean isConst();

    boolean isReadable();

    boolean isWritable();
}
