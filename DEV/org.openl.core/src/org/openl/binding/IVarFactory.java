/*
 * Created on Jun 3, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding;

import org.openl.binding.exception.AmbiguousFieldException;
import org.openl.types.IOpenField;

/**
 * @author snshor
 */

public interface IVarFactory {

    IOpenField getVar(String name, boolean strictMatch) throws AmbiguousFieldException;

}
