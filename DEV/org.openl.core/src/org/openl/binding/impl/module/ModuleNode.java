/*
 * Created on Jul 24, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl.module;

import org.openl.binding.BindingDependencies;
import org.openl.binding.IBoundNode;
import org.openl.binding.impl.ABoundNode;
import org.openl.exception.OpenLRuntimeException;
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

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.IBoundNode#evaluate(org.openl.vm.IRuntimeEnv)
     */
    public Object evaluateRuntime(IRuntimeEnv env) throws OpenLRuntimeException {
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

    @Override
    public void updateDependency(BindingDependencies dependencies) {
        // TODO Auto-generated method stub

    }

    // /* (non-Javadoc)
    // * @see org.openl.binding.IBoundModuleNode#getMethodNode(java.lang.String)
    // */
    // public IBoundMethodNode getMethodNode(String name)
    // {
    // for (int i = 0; i < children.length; i++)
    // {
    // MethodNode mnode = (MethodNode)children[i];
    // if (mnode.getName().equals(name))
    // return mnode;
    // }
    // return null;
    // }

}
