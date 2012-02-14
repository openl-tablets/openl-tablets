/*
 * Created on May 29, 2003 Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.impl.LiteralNode;
import org.openl.types.IOpenClass;
import org.openl.types.java.JavaOpenClass;

/**
 * @author snshor
 * 
 */
public class IntNodeBinder extends ANodeBinder {

    /*
     * (non-Javadoc)
     * @see org.openl.binding.INodeBinder#bind(org.openl.parser.ISyntaxNode, org.openl.env.IOpenEnv,
     * org.openl.binding.IBindingContext)
     */
    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) {

        String s = ((LiteralNode) node).getImage();

        if (s.charAt(0) == '$') {
            s = s.substring(1);
        }

        if (s.charAt(0) == '+') {
            s = s.substring(1);
        }

        int len = s.length();

        if (Character.toUpperCase(s.charAt(len - 1)) == 'L') {
            return new LiteralBoundNode(node, Long.decode(s.substring(0, len - 1)), JavaOpenClass.LONG);
        }

        return new LiteralBoundNode(node, Integer.decode(s), JavaOpenClass.INT);
    }
    
    @Override
    public IBoundNode bindType(ISyntaxNode node, IBindingContext bindingContext, IOpenClass type) throws Exception {
        IBoundNode boundNode = bindChildNode(node, bindingContext);
        IOpenCast cast = getCast(boundNode, type, bindingContext, false);

        if (cast == null) {
            return boundNode;
        }

        return new CastNode(null, boundNode, cast, type);
    }
}
