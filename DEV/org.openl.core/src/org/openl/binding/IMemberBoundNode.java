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

    void finalizeBind(IBindingContext cxt) throws Exception;

    void removeDebugInformation(IBindingContext cxt) throws Exception;
}
