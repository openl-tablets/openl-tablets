package org.openl.binding.impl.module;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.binding.IMemberBoundNode;
import org.openl.binding.impl.ABoundNode;
import org.openl.binding.impl.cast.IOpenCast;
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
    @Override
    public void addTo(ModuleOpenClass openClass) {

        openClass.addField(field);
        openClass.addInitializerNode(this);
        if (field instanceof DynamicObjectField) {
            ((DynamicObjectField) field).setDeclaringClass(openClass);
        }

    }

    @Override
    protected Object evaluateRuntime(IRuntimeEnv env) {
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
    @Override
    public void finalizeBind(IBindingContext cxt) throws Exception {
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.IBoundNode#getType()
     */
    @Override
    public IOpenClass getType() {
        return JavaOpenClass.VOID;
    }

    @Override
    public void removeDebugInformation(IBindingContext cxt) throws Exception {
        // nothing to remove
    }

}
