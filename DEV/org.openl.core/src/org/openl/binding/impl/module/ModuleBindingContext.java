/*
 * Created on Jul 28, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl.module;

import org.openl.binding.IBindingContext;
import org.openl.binding.impl.component.ComponentBindingContext;

/**
 * Common binding context for full module.
 * 
 * @author snshor
 * 
 */
public class ModuleBindingContext extends ComponentBindingContext {

    public ModuleBindingContext(IBindingContext delegate, ModuleOpenClass module) {
        super(delegate, module);
    }

    public ModuleOpenClass getModule() {
        return (ModuleOpenClass) getComponentOpenClass();
    }
}
