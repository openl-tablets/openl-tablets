package org.openl.binding.impl;

import org.jspecify.annotations.Nullable;

import org.openl.binding.BindingDependencies;
import org.openl.binding.IBoundNode;
import org.openl.binding.ILocalVar;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IOpenClass;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.ArrayTool;
import org.openl.vm.IRuntimeEnv;

/**
 * The declaration of a local variable, with the value it starts with.
 * <p>
 * The written type of the variable is a child of the declaration, so the editor shows its hint and its link. It is
 * never evaluated.
 */
public final class LocalVarDeclarationNode extends ABoundNode {

    private final ILocalVar localVar;
    private final @Nullable IBoundNode initNode;

    LocalVarDeclarationNode(ISyntaxNode syntaxNode,
                            @Nullable IBoundNode typeNode,
                            @Nullable IBoundNode initNode,
                            ILocalVar localVar) {
        super(syntaxNode, ArrayTool.removeNulls(new IBoundNode[]{typeNode, initNode}));

        this.initNode = initNode;
        this.localVar = localVar;
    }

    @Override
    protected Object evaluateRuntime(IRuntimeEnv env) {
        Object initObj = initNode == null ? null : initNode.evaluate(env);

        env.getLocalFrame()[localVar.getIndexInLocalFrame()] = initObj;
        return null;
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
    public void updateDependency(BindingDependencies dependencies) {
        dependencies.addFieldDependency(localVar, this);
    }

}
