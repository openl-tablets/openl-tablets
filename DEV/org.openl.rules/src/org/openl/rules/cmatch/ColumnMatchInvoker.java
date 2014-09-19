package org.openl.rules.cmatch;

import org.openl.rules.method.RulesMethodInvoker;
import org.openl.types.IOpenClass;
import org.openl.vm.IRuntimeEnv;

/**
 * @author Yury Molchan
 */
public class ColumnMatchInvoker extends RulesMethodInvoker {

    public ColumnMatchInvoker(ColumnMatch columnMatch) {
        super(columnMatch);
    }

    @Override
    public ColumnMatch getInvokableMethod() {
        return (ColumnMatch) super.getInvokableMethod();
    }

    @Override
    public boolean canInvoke() {
        return getInvokableMethod().getAlgorithmExecutor() != null;
    }

    @Override
    public Object invokeSimple(Object target, Object[] params, IRuntimeEnv env) {
        Object result = getInvokableMethod().getAlgorithmExecutor().invoke(target, params, env, getInvokableMethod());

        if (result == null) {
            IOpenClass type = getInvokableMethod().getHeader().getType();
            if (type.getInstanceClass().isPrimitive()) {
                throw new IllegalArgumentException("Cannot return <null> for primitive type " + type.getInstanceClass()
                        .getName());
            }
        }

        return result;
    }
}
