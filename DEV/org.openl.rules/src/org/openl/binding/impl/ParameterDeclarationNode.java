package org.openl.binding.impl;

import lombok.Getter;

import org.openl.binding.IBoundParameterDeclaration;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IOpenClass;
import org.openl.types.IParameterDeclaration;
import org.openl.vm.IRuntimeEnv;

public class ParameterDeclarationNode extends ABoundNode implements IBoundParameterDeclaration {

    @Getter
    private final IParameterDeclaration parameterDeclaration;

    ParameterDeclarationNode(ISyntaxNode syntaxNode, IParameterDeclaration parameterDeclaration) {
        super(syntaxNode);
        this.parameterDeclaration = parameterDeclaration;
    }

    @Override
    protected Object evaluateRuntime(IRuntimeEnv env) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IOpenClass getType() {
        return parameterDeclaration.getType();
    }
}
