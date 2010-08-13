/*
 * Created on Jun 16, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding;

import org.openl.types.IOpenClass;

/**
 * @author snshor
 *
 */
public interface INameSpacedTypeFactory {

    public IOpenClass getType(String namespace, String typename);
}
