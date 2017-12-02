package org.openl.binding.impl.module;

import org.openl.binding.IBoundNode;
import org.openl.binding.impl.ABoundNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IOpenClass;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 *
 */
public class ModuleNode extends ABoundNode // implements IBoundModuleNode
{

    IOpenClass type;

    /**
     * @param syntaxNode
     * @param children
     */
    public ModuleNode(ISyntaxNode syntaxNode, IOpenClass type) {
        super(syntaxNode, new IBoundNode[0]);
        this.type = type;
    }

    @Override
    protected Object evaluateRuntime(IRuntimeEnv env) {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.IBoundNode#getType()
     */
    public IOpenClass getType() {
        return type;
    }
}
