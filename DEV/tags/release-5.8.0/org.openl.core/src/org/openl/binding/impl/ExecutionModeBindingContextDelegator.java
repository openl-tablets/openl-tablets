package org.openl.binding.impl;

import org.openl.binding.IBindingContext;

/**
 * Binding context delegator for "Execution Mode".
 * 
 * @author PUdalau
 */
public class ExecutionModeBindingContextDelegator extends BindingContextDelegator {
    public ExecutionModeBindingContextDelegator(IBindingContext delegate) {
        super(delegate);
    }
    
    @Override
    public boolean isExecutionMode() {
        return true;
    }
}
