package org.openl.rules.method.table;

import org.openl.rules.method.RulesMethodInvoker;
import org.openl.vm.IRuntimeEnv;

/**
 * Invoker for {@link TableMethod}.
 *
 * @author Yury Molchan
 */
public class MethodTableInvoker extends RulesMethodInvoker<TableMethod> {

    public MethodTableInvoker(TableMethod tableMethod) {
        super(tableMethod);
    }

    @Override
    public Object invokeSimple(Object target, Object[] params, IRuntimeEnv env) {
        return getInvokableMethod().getCompositeMethod().invoke(target, params, env);
    }

    @Override
    public boolean canInvoke() {
        return getInvokableMethod().getCompositeMethod().isInvokable();
    }
}
