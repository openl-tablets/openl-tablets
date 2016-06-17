package org.openl.rules.webstudio.web.test;

import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.context.RulesRuntimeContextDelegator;

/**
 * This class is needed only to give a human-readable name to the runtime context parameter. Used in UI only.
 */
public class Context extends RulesRuntimeContextDelegator {
    // Stub. Needed for ParameterTreeBuilder to draw this class as a simple java bean
    public Context() {
        super(null);
    }

    public Context(IRulesRuntimeContext delegate) {
        super(delegate);
    }
}
