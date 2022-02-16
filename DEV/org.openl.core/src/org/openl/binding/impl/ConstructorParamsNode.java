package org.openl.binding.impl;

import org.openl.binding.MethodUtil;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.vm.IRuntimeEnv;

/**
 * constructor with parameters
 *
 * Example:
 * <p>
 * ObjectType(value1, value2, ObjectType2(value3);
 * </p>
 *
 * @author Eugene Biruk
 */
public class ConstructorParamsNode extends ABoundNode implements ConstructorNode {

    private final MethodBoundNode constructor;

    public ConstructorParamsNode(MethodBoundNode constructor) {
        super(constructor.getSyntaxNode(), constructor.getChildren());
        this.constructor = constructor;
    }

    @Override
    public MethodBoundNode getConstructor() {
        return constructor;
    }

    @Override
    public String getDescription() {
        IOpenMethod method = constructor.getMethodCaller().getMethod();
        return MethodUtil.printConstructor(method);
    }

    @Override
    protected Object evaluateRuntime(IRuntimeEnv env) throws Exception {
        return constructor.evaluate(env);
    }

    @Override
    public IOpenClass getType() {
        return constructor.getType();
    }
}
