package org.openl.binding.impl.module;

import org.openl.binding.impl.ABoundNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IOpenClass;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 *
 */
public class ModuleNode extends ABoundNode {

    IOpenClass type;

    public ModuleNode(ISyntaxNode syntaxNode, IOpenClass type) {
        super(syntaxNode);
        this.type = type;
    }

    @Override
    protected Object evaluateRuntime(IRuntimeEnv env) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IOpenClass getType() {
        return type;
    }
}
