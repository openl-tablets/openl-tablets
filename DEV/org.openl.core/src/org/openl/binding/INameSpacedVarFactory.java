/*
 * Created on Jun 12, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding;

import org.openl.binding.exception.AmbiguousVarException;
import org.openl.types.IOpenField;

/**
 * @author snshor
 *
 */
public interface INameSpacedVarFactory {
    IOpenField getVar(String namespace, String name, boolean strictMatch) throws AmbiguousVarException;
}
