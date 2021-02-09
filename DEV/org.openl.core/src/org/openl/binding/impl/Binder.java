package org.openl.binding.impl;

import java.util.HashMap;
import java.util.Map;

import org.openl.IOpenBinder;
import org.openl.OpenL;
import org.openl.binding.*;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.code.IParsedCode;
import org.openl.types.impl.MethodKey;
import org.openl.types.java.JavaOpenClass;

/**
 * Default implementation of {@link IOpenBinder}.
 *
 * @author Yury Molchan
 */
public class Binder implements IOpenBinder {

    final Map<MethodKey, Object> methodCache = new HashMap<>();
    private final OpenL openl;
    private final INodeBinderFactory nodeBinderFactory;
    private final ICastFactory castFactory;
    private final INameSpacedVarFactory varFactory;
    private final INameSpacedTypeFactory typeFactory;
    private final INameSpacedMethodFactory methodFactory;

    public Binder(INodeBinderFactory nodeBinderFactory,
            INameSpacedMethodFactory methodFactory,
            ICastFactory castFactory,
            INameSpacedVarFactory varFactory,
            INameSpacedTypeFactory typeFactory,
            OpenL openl) {

        this.nodeBinderFactory = nodeBinderFactory;
        this.methodFactory = methodFactory;
        this.castFactory = castFactory;
        this.varFactory = varFactory;
        this.typeFactory = typeFactory;
        this.openl = openl;
    }

    @Override
    public ICastFactory getCastFactory() {
        return castFactory;
    }

    @Override
    public INameSpacedMethodFactory getMethodFactory() {
        return methodFactory;
    }

    @Override
    public INodeBinderFactory getNodeBinderFactory() {
        return nodeBinderFactory;
    }

    @Override
    public INameSpacedTypeFactory getTypeFactory() {
        return typeFactory;
    }

    @Override
    public INameSpacedVarFactory getVarFactory() {
        return varFactory;
    }

    @Override
    public IBindingContext makeBindingContext() {
        return new BindingContext(this, JavaOpenClass.VOID, openl);
    }

    @Override
    public IBoundCode bind(IParsedCode parsedCode) {
        return bind(parsedCode, null);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.IOpenBinder#bind(org.openl.syntax.IParsedCode)
     */
    @Override
    public IBoundCode bind(IParsedCode parsedCode, IBindingContext bindingContext) {
        if (bindingContext == null) {
            bindingContext = makeBindingContext();
        }

        ISyntaxNode syntaxNode = parsedCode.getTopNode();
        IBoundNode boundNode;
        try {
            bindingContext.pushLocalVarContext();
            boundNode = ANodeBinder.bindChildNode(syntaxNode, bindingContext);
        } finally {
            bindingContext.popLocalVarContext();
        }
        return new BoundCode(parsedCode, boundNode, bindingContext.getErrors(), bindingContext.getMessages());
    }
}
