package org.openl.binding.impl;

import org.openl.binding.IBoundParameterDeclaration;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IOpenClass;
import org.openl.types.IParameterDeclaration;
import org.openl.vm.IRuntimeEnv;

public class ParameterDeclarationNode extends ABoundNode implements IBoundParameterDeclaration {

    private final IParameterDeclaration parameterDeclaration;

    ParameterDeclarationNode(ISyntaxNode syntaxNode, IParameterDeclaration parameterDeclaration) {
        super(syntaxNode);
        this.parameterDeclaration = parameterDeclaration;
    }

    @Override
    protected Object evaluateRuntime(IRuntimeEnv env) {
        throw new UnsupportedOperationException();
    }

    public IParameterDeclaration getParameterDeclaration() {
        return parameterDeclaration;
    }

    @Override
    public IOpenClass getType() {
        return parameterDeclaration.getType();
    }
}
