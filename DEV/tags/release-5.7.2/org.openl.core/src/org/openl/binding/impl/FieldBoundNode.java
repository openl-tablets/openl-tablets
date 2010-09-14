/*
 * Created on May 19, 2003 Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl;

import org.openl.binding.BindingDependencies;
import org.openl.binding.IBoundNode;
import org.openl.exception.OpenLRuntimeException;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 * 
 */
public class FieldBoundNode extends ATargetBoundNode {

    IOpenField boundField;

    public FieldBoundNode(ISyntaxNode syntaxNode, IOpenField field) {
        super(syntaxNode, new IBoundNode[0]);
        boundField = field;
    }

    public FieldBoundNode(ISyntaxNode syntaxNode, IOpenField field, IBoundNode target) {
        super(syntaxNode, new IBoundNode[0], target);
        boundField = field;
    }

    /*
     * (non-Javadoc)
     * @see org.openl.binding.IBoundNode#assign(java.lang.Object)
     */
    @Override
    public void assign(Object value, IRuntimeEnv env) throws OpenLRuntimeException {
        Object target = targetNode == null ? env.getThis() : targetNode.evaluate(env);

        boundField.set(target, value, env);
    }

    /*
     * (non-Javadoc)
     * @see org.openl.binding.IBoundNode#invoke(java.lang.Object[])
     */
    // public Object evaluate(Object target, Object[] pars, IRuntimeEnv env)
    // {
    // return boundField.get(target);
    // }
    //
    /*
     * (non-Javadoc)
     * @see org.openl.binding.IBoundNode#evaluate(org.openl.vm.IRuntimeEnv)
     */
    public Object evaluateRuntime(IRuntimeEnv env) throws OpenLRuntimeException {
        Object target = targetNode == null ? env.getThis() : targetNode.evaluate(env);

        return boundField.get(target, env);
    }

    /*
     * (non-Javadoc)
     * @see org.openl.binding.IBoundNode#getType()
     */
    public IOpenClass getType() {
        return boundField.getType();
    }

    /*
     * (non-Javadoc)
     * @see org.openl.binding.IBoundNode#isLvalue()
     */
    @Override
    public boolean isLvalue() {
        return boundField.isWritable();
    }

    @Override
    public void updateAssignFieldDependency(BindingDependencies dependencies) {
        dependencies.addAssignField(boundField, this);
    }

    @Override
    public void updateDependency(BindingDependencies dependencies) {
        dependencies.addFieldDependency(boundField, this);
    }

    @Override
    public boolean isLiteralExpressionParent() {
        return boundField.isConst();
    }

}
