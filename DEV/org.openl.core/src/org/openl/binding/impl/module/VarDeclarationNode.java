package org.openl.binding.impl.module;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.binding.IMemberBoundNode;
import org.openl.binding.impl.ABoundNode;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IOpenClass;
import org.openl.types.impl.DynamicObjectField;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.IRuntimeEnv;

public final class VarDeclarationNode extends ABoundNode implements IMemberBoundNode {

    DynamicObjectField field;

    IOpenCast cast;

    IBoundNode initNode;

    VarDeclarationNode(ISyntaxNode syntaxNode, IBoundNode initNode, DynamicObjectField field, IOpenCast cast) {
        super(syntaxNode, initNode);

        this.initNode = initNode;
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
        field.setDeclaringClass(openClass);
    }

    @Override
    protected Object evaluateRuntime(IRuntimeEnv env) {
        Object init = initNode == null ? null : initNode.evaluate(env);

        Object initObj = init == null ? field.getType().nullObject() : init;

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
