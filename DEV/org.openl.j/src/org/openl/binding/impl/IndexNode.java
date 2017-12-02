package org.openl.binding.impl;

import org.openl.binding.BindingDependencies;
import org.openl.binding.IBoundNode;
import org.openl.exception.OpenLRuntimeException;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenIndex;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 *
 */
public class IndexNode extends ATargetBoundNode {

    IOpenIndex index;

    /**
     * @param syntaxNode
     * @param children
     * @param targetNode
     */
    public IndexNode(ISyntaxNode syntaxNode, IBoundNode[] children, IBoundNode targetNode, IOpenIndex index) {
        super(syntaxNode, children, targetNode);
        this.index = index;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.IBoundNode#assign(java.lang.Object)
     */
    @Override
    public void assign(Object value, IRuntimeEnv env) throws OpenLRuntimeException {
        Object target = getTargetNode().evaluate(env);

        index.setValue(target, children[0].evaluate(env), value);
    }

    @Override
    protected Object evaluateRuntime(IRuntimeEnv env) {
        return index.getValue(getTargetNode().evaluate(env), children[0].evaluate(env));
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.IBoundNode#getType()
     */
    public IOpenClass getType() {
        return index.getElementType();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.IBoundNode#isLvalue()
     */
    @Override
    public boolean isLvalue() {
        return index.isWritable();
    }

    @Override
    public void updateAssignFieldDependency(BindingDependencies dependencies) {
        getTargetNode().updateAssignFieldDependency(dependencies);
    }

}
