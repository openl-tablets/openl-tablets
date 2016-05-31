/*
 * Created on Jun 16, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.types;

import org.openl.binding.exception.AmbiguousTypeException;

/**
 * @author snshor
 *
 */
public interface ITypeLibrary {

    IOpenClass getType(String typename) throws AmbiguousTypeException;
}
