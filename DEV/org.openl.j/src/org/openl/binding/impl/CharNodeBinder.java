/*
 * Created on Jun 6, 2003 Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.impl.LiteralNode;
import org.openl.types.java.JavaOpenClass;

/**
 * @author snshor
 * 
 */
public class CharNodeBinder extends ANodeBinder {

    /*
     * (non-Javadoc)
     * @see org.openl.binding.INodeBinder#bind(org.openl.parser.ISyntaxNode, org.openl.env.IOpenEnv,
     * org.openl.binding.IBindingContext)
     */
    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) throws Exception {
        String s = ((LiteralNode) node).getImage();
        char c = s.charAt(1);

        try {
            if (c == '\\') {
                char nextC = s.charAt(2);
                switch (nextC) {
                    case 'b':
                        c = '\b';
                        break;
                    case 't':
                        c = '\t';
                        break;
                    case 'n':
                        c = '\n';
                        break;
                    case 'f':
                        c = '\f';
                        break;
                    case 'r':
                        c = '\r';
                        break;
                    case '"':
                        c = '"';
                        break;
                    case '\'':
                        c = '\'';
                        break;
                    case '\\':
                        c = '\\';
                        break;
                    case 'u':
                        c = StringNodeBinder.processUnicode(s, 3);
                        break;
                    case '0':
                    case '1':
                    case '2':
                    case '3':
                    case '4':
                    case '5':
                    case '6':
                    case '7': {
                        int res = 0;
                        int i;
                        for (i = 0; i < 3; ++i) {
                            char cc = s.charAt(2 + i);

                            if ('0' <= cc && cc <= '7') {
                                res = res * 8 + cc - '0';
                            } else {
                                break;
                            }
                            c = (char) res;
                        }

                        break;
                    }
                }
            }

        } catch (Exception ex) {
            return makeErrorNode(ex, node, bindingContext);
        }

        return new LiteralBoundNode(node, new Character(c), JavaOpenClass.CHAR);
    }

}
