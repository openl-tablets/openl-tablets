package org.openl.binding.impl.module;

import lombok.Getter;

import org.openl.binding.impl.ABoundNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IOpenClass;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 */
public class ModuleNode extends ABoundNode {

    @Getter
    private final IOpenClass type;

    public ModuleNode(ISyntaxNode syntaxNode, IOpenClass type) {
        super(syntaxNode);
        this.type = type;
    }

    @Override
    protected Object evaluateRuntime(IRuntimeEnv env) {
        throw new UnsupportedOperationException();
    }
}
