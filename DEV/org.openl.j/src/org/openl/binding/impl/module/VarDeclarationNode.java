/*
 * Created on Jun 16, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl.module;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.binding.IMemberBoundNode;
import org.openl.binding.impl.ABoundNode;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.exception.OpenLRuntimeException;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.impl.DynamicObjectField;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 *
 */
public class VarDeclarationNode extends ABoundNode implements IMemberBoundNode {

    IOpenField field;

    IOpenCast cast;

    /**
     * @param syntaxNode
     * @param children
     */
    public VarDeclarationNode(ISyntaxNode syntaxNode, IBoundNode[] children, IOpenField field, IOpenCast cast) {
        super(syntaxNode, children);

        this.field = field;
        this.cast = cast;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.impl.module.IMemberBoundNode#addTo(org.openl.binding.impl.module.ModuleOpenClass)
     */
    public void addTo(ModuleOpenClass openClass) {

        openClass.addField(field);
        openClass.addInitializerNode(this);
        if (field instanceof DynamicObjectField) {
            ((DynamicObjectField) field).setDeclaringClass(openClass);
        }

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
     * @see org.openl.binding.IBoundNode#evaluate(org.openl.vm.IRuntimeEnv)
     */
    public Object evaluateRuntime(IRuntimeEnv env) throws OpenLRuntimeException {
        Object[] init = evaluateChildren(env);

        Object initObj = init == null || init.length == 0 ? field.getType().nullObject() : init[0];

        initObj = cast == null ? initObj : cast.convert(initObj);

        field.set(env.getThis(), initObj, env);
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.impl.module.IMemberBoundNode#finalizeBind(org.openl.binding.IBindingContext)
     */
    public void finalizeBind(IBindingContext cxt) throws Exception {
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.IBoundNode#getType()
     */
    public IOpenClass getType() {
        return JavaOpenClass.VOID;
    }

    public void removeDebugInformation(IBindingContext cxt) throws Exception {
        //nothing to remove
    }
    
}
