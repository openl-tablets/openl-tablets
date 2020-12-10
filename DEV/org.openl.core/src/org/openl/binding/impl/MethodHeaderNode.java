package org.openl.binding.impl;

import org.openl.binding.IBoundMethodHeader;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethodHeader;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 *
 */
public class MethodHeaderNode extends ABoundNode implements IBoundMethodHeader {

    final IOpenMethodHeader methodHeader;

    MethodHeaderNode(ISyntaxNode syntaxNode, IOpenMethodHeader methodHeader) {
        super(syntaxNode);
        this.methodHeader = methodHeader;
    }

    @Override
    protected Object evaluateRuntime(IRuntimeEnv env) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IOpenMethodHeader getMethodHeader() {
        return methodHeader;
    }

    @Override
    public IOpenClass getType() {
        return methodHeader.getType();
    }
}
