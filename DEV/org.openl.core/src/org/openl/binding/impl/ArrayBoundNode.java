package org.openl.binding.impl;

import java.lang.reflect.Array;

import org.openl.binding.IBoundNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IOpenClass;
import org.openl.vm.IRuntimeEnv;

/**
 * Creates an array instances of the following format:
 *
 * <li>new int[5]</li>
 * <li>new int[5][3]</li>
 * <li>new int[5][]</li>
 * <li>new int[5][][]</li>
 *
 * Not supported:
 *
 * <li>new int[] {}</li>
 * <li>new int[] {10}</li>
 * <li>new int[][] {}</li>
 * <li>new int[][] {{}, {10} }</li>
 *
 * @author Yury Molchan
 */
final class ArrayBoundNode extends ABoundNode {

    private final IOpenClass arrayType;

    ArrayBoundNode(ISyntaxNode syntaxNode, IBoundNode[] children, IOpenClass arrayType) {
        super(syntaxNode, children);
        this.arrayType = arrayType;
    }

    @Override
    protected Object evaluateRuntime(IRuntimeEnv env) {
        int[] dims = new int[children.length];
        IOpenClass componentType = arrayType;

        for (int i = 0; i < children.length; i++) {
            dims[i] = (Integer) children[i].evaluate(env);
            componentType = componentType.getComponentClass();
        }

        Class<?> componentClass = componentType.getInstanceClass();
        return Array.newInstance(componentClass, dims);
    }

    @Override
    public IOpenClass getType() {
        return arrayType;
    }
}
