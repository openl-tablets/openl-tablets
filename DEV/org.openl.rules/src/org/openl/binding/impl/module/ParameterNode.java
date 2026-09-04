package org.openl.binding.impl.module;

import lombok.Getter;

import org.openl.binding.impl.ABoundNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IOpenClass;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 */
public class ParameterNode extends ABoundNode {

    @Getter
    protected final String name;
    @Getter
    protected final IOpenClass type;
    @Getter
    protected final String contextProperty;

    ParameterNode(ISyntaxNode syntaxNode, String name, IOpenClass type) {
        this(syntaxNode, name, type, null);
    }

    ParameterNode(ISyntaxNode syntaxNode, String name, IOpenClass type, String contextProperty) {
        super(syntaxNode);
        this.name = name;
        this.type = type;
        this.contextProperty = contextProperty;
    }

    @Override
    protected Object evaluateRuntime(IRuntimeEnv env) {
        throw new UnsupportedOperationException();
    }
}
