/*
 * Created on Sep 23, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding;

import org.openl.binding.impl.module.ModuleOpenClass;

/**
 * @author snshor
 */
public interface IMemberBoundNode {

    void addTo(ModuleOpenClass openClass);

    // Implemented by every table bound node, whose binding steps throw many different checked exceptions.
    @SuppressWarnings("java:S112")
    void finalizeBind(IBindingContext cxt) throws Exception;

    // Implemented by every table bound node, whose binding steps throw many different checked exceptions.
    @SuppressWarnings("java:S112")
    void removeDebugInformation(IBindingContext cxt) throws Exception;
}
