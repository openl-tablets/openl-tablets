package org.openl.binding.impl;

import org.openl.binding.IBoundNode;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IOpenClass;

/**
 * This cast node is designed as a marker class and used only for hands written casts in an expression. If cast is
 * exists in an expression then TypeCastNode should be used otherwise CastNode should be used.
 */
public class TypeCastNode extends CastNode {

    public TypeCastNode(ISyntaxNode castSyntaxNode, IBoundNode bnode, IOpenCast cast, IOpenClass castedType) {
        super(castSyntaxNode, bnode, cast, castedType);
    }

}
