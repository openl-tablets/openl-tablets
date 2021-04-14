package org.openl.binding.impl;

import org.openl.binding.MethodUtil;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.java.JavaOpenClass;
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
    private final boolean isShort;

    public ConstructorParamsNode(MethodBoundNode constructor, boolean isShort) {
        super(constructor.getSyntaxNode(), constructor.getChildren());
        this.constructor = constructor;
        this.isShort = isShort;
    }

    @Override
    public MethodBoundNode getConstructor() {
        return constructor;
    }

    @Override
    public String getDescription() {
        StringBuilder buff = new StringBuilder();
        IOpenMethod method = constructor.getMethodCaller().getMethod();
        if (method.getDeclaringClass() instanceof JavaOpenClass) {
            buff.append(method.getDeclaringClass().getPackageName()).append('\n');
        }
        MethodUtil.printConstructor(method, buff);
        return buff.toString();
    }

    @Override
    public boolean isShort() {
        return isShort;
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
