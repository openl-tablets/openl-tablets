/*
 * Created on Jul 10, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl;

import org.openl.binding.BindingDependencies;
import org.openl.binding.IBoundNode;
import org.openl.exception.OpenLRuntimeException;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IOpenClass;
import org.openl.vm.IRuntimeEnv;

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

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.IBoundNode#evaluate(org.openl.vm.IRuntimeEnv)
     */
    public Object evaluateRuntime(IRuntimeEnv env) throws OpenLRuntimeException {
        Object[] res = evaluateChildren(env);

        int[] dims = new int[dimensions + res.length];

        for (int i = 0; i < res.length; i++) {
            dims[i] = ((Integer) res[i]).intValue();
        }

        return componentType.getAggregateInfo().makeIndexedAggregate(componentType, dims);
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
