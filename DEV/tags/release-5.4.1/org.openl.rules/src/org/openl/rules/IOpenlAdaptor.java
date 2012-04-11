/*
 * Created on Jun 23, 2004
 *
 * Developed by OpenRules Inc 2003-2004
 */
package org.openl.rules;

import org.openl.source.IOpenSourceCodeModule;
import org.openl.types.impl.CompositeMethod;

/**
 * @author snshor
 *
 * The purpose of this interface is to simplify compiling of OpenL objects in
 * complex structured environments where context is defined on top and must be
 * propagated down without having to transfer many of the elements required to
 * do the validation and compilaton
 */
public interface IOpenlAdaptor {

    public CompositeMethod makeMethod(IOpenSourceCodeModule src);

}
