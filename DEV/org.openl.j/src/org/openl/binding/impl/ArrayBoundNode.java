package org.openl.binding.impl;

import org.openl.binding.BindingDependencies;
import org.openl.binding.IBoundNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IOpenClass;
import org.openl.vm.IRuntimeEnv;

import java.lang.reflect.Array;

/**
 * @author snshor
 *
 */
public class ArrayBoundNode extends ABoundNode {

    int dimensions;
    IOpenClass arrayType;
    IOpenClass componentType;

    /**
     * @param syntaxNode
     * @param children
     */
    public ArrayBoundNode(ISyntaxNode syntaxNode, IBoundNode[] children, int dimensions, IOpenClass arrayType,
            IOpenClass componentType) {
        super(syntaxNode, children);
        this.dimensions = dimensions;
        this.arrayType = arrayType;
        this.componentType = componentType;
    }

    @Override
    protected Object evaluateRuntime(IRuntimeEnv env) {
        Object[] res = evaluateChildren(env);

        int[] dims = new int[dimensions + res.length];

        for (int i = 0; i < res.length; i++) {
            dims[i] = (Integer) res[i];
        }

        return Array.newInstance(componentType.getInstanceClass(), dims);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.IBoundNode#getType()
     */
    public IOpenClass getType() {
        return arrayType;
    }

    @Override
    public void updateDependency(BindingDependencies dependencies) {
        dependencies.addTypeDependency(componentType, this);
    }

}
