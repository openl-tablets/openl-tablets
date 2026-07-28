package org.openl.binding.impl;

import java.util.Arrays;

import org.openl.binding.IBoundNode;
import org.openl.binding.ILocalVar;
import org.openl.binding.MethodUtil;
import org.openl.types.IOpenClass;
import org.openl.util.StreamUtils;
import org.openl.vm.IRuntimeEnv;

/**
 * constructor with assignment of values to fields by names
 * <p>
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
        var evaluate = constructor.evaluate(env);
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
        var method = constructor.getMethodCaller().getMethod();

        var params = Arrays.stream(getChildren()).map(node -> node.getChildren()[0])
                .collect(StreamUtils.toLinkedMap(node -> node.getSyntaxNode().getText(), IBoundNode::getType));

        return MethodUtil.printConstructorWithNamedParameters(method, params);
    }
}
