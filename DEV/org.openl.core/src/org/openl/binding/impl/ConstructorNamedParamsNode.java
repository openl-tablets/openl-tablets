package org.openl.binding.impl;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.openl.binding.IBoundNode;
import org.openl.binding.ILocalVar;
import org.openl.binding.MethodUtil;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.IRuntimeEnv;

/**
 * constructor with assignment of values to fields by names
 *
 * Example:
 * <p>
 * ObjectType(field1Name=value1, field2Name=value2, field3Name=ObjectType2(field1=value3);
 * </p>
 *
 * @author Eugene Biruk
 */
public class ConstructorNamedParamsNode extends ABoundNode implements ConstructorNode {

    private final ILocalVar tempVar;
    private final MethodBoundNode constructor;

    public ConstructorNamedParamsNode(ILocalVar tempVar, MethodBoundNode constructor, IBoundNode... children) {
        super(constructor.getSyntaxNode(), children);
        this.tempVar = tempVar;
        this.constructor = constructor;
    }

    @Override
    protected Object evaluateRuntime(IRuntimeEnv env) {
        Object evaluate = constructor.evaluate(env);
        tempVar.set(null, evaluate, env);
        for (IBoundNode child : children) {
            child.evaluate(env);
        }
        return evaluate;
    }

    @Override
    public IOpenClass getType() {
        return tempVar.getType();
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
        Map<String, String> params = Arrays.stream(getChildren()).collect(Collectors.toMap(e -> e.getChildren()[0].getSyntaxNode().getText(), e -> e.getChildren()[0].getType().getName(), (e1, e2) -> e1, LinkedHashMap::new));
        MethodUtil.printConstructorWithNamedParameters(method, params, buff);
        return buff.toString();
    }

    @Override
    public boolean isShort() {
        return false;
    }
}
