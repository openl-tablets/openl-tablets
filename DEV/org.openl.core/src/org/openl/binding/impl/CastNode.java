package org.openl.binding.impl;

import org.openl.binding.IBoundNode;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.exception.OpenLRuntimeException;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IOpenClass;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 *
 */
public class CastNode extends ABoundNode {

    IOpenCast cast;
    IOpenClass castedType;

    /**
     * @param syntaxNode
     * @param children
     */
    public CastNode(ISyntaxNode castSyntaxNode, IBoundNode bnode, IOpenCast cast, IOpenClass castedType) {
        super(castSyntaxNode == null ? bnode.getSyntaxNode() : castSyntaxNode, new IBoundNode[] { bnode });
        this.cast = cast;
        this.castedType = castedType;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.IBoundNode#assign(java.lang.Object,
     *      org.openl.vm.IRuntimeEnv)
     */
    @Override
    public void assign(Object value, IRuntimeEnv env) throws OpenLRuntimeException {
        children[0].assign(value, env);
    }

    @Override
    protected Object evaluateRuntime(IRuntimeEnv env) {
        Object res = children[0].evaluate(env);
        return cast.convert(res);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.IBoundNode#getTargetNode()
     */
    @Override
    public IBoundNode getTargetNode() {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.IBoundNode#getType()
     */
    @Override
    public IOpenClass getType() {
        return castedType;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.IBoundNode#isLvalue()
     */
    @Override
    public boolean isLvalue() {
        return children[0].isLvalue();
    }
}
