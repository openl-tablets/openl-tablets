/*
 * Created on Jun 16, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl;

import org.openl.binding.BindingDependencies;
import org.openl.binding.IBoundNode;
import org.openl.binding.ILocalVar;
import org.openl.exception.OpenLRuntimeException;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IOpenClass;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 *
 */
public class LocalVarDeclarationNode extends ABoundNode {

    ILocalVar var;

    // IOpenCast cast;

    /**
     * @param syntaxNode
     * @param children
     */
    public LocalVarDeclarationNode(ISyntaxNode syntaxNode, IBoundNode[] children, ILocalVar var) {
        super(syntaxNode, children);

        this.var = var;
        // this.cast = cast;
    }

    public Object evaluateRuntime(IRuntimeEnv env) throws OpenLRuntimeException {
        Object[] init = evaluateChildren(env);

        Object initObj = init == null || init.length == 0 ? null : init[0];

        // / initObj = cast == null ? initObj : cast.convert(initObj);

        env.getLocalFrame()[var.getIndexInLocalFrame()] = initObj;
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.IBoundNode#evaluate(java.lang.Object,
     *      java.lang.Object[], org.openl.vm.IRuntimeEnv)
     */
    // public Object evaluate(Object target, Object[] pars, IRuntimeEnv env)
    // {
    // Object[] localFrame = env.getLocalFrame();
    //
    // localFrame[var.getIndexInLocalFrame()] = pars == null ? null : pars[0];
    // return null;
    // }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.IBoundNode#getType()
     */
    public IOpenClass getType() {
        return JavaOpenClass.VOID;
    }

    @Override
    public void updateDependency(BindingDependencies dependencies) {
        dependencies.addTypeDependency(var.getType(), this);
        dependencies.addFieldDependency(var, this);
    }

}
