/*
 * Created on May 9, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.types;

import org.openl.base.INamedThing;

/**
 * @author snshor
 *
 */
public interface IOpenMember extends INamedThing {
    public IOpenClass getDeclaringClass();

    public IMemberMetaInfo getInfo();

    public IOpenClass getType();

    public boolean isStatic();

}
