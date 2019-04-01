/*
 * Created on Jun 6, 2003 Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.java.JavaOpenClass;

/**
 * @author snshor
 *
 */
public class StringNodeBinder extends ANodeBinder {

    public static int processOctal(String s, int startIndex, StringBuilder buf) {

        int res = 0;
        int i;

        for (i = 0; i < 3; ++i) {
            char c = s.charAt(startIndex + i);

            if ('0' <= c && c <= '7') {
                res = res * 8 + c - '0';
            } else {
                break;
            }
        }

        buf.append((char) res);

        return i;
    }

    public static char processUnicode(String s, int startIndex) throws Exception {

        int res = 0;

        for (int i = 0; i < 4; ++i) {
            char c = s.charAt(startIndex + i);

            if ('0' <= c && c <= '9') {
                res = res * 16 + c - '0';
            } else if ('a' <= c && c <= 'f') {
                res = res * 16 + c - 'a';
            } else if ('A' <= c && c <= 'F') {
                res = res * 16 + c - 'A';
            } else {
                throw new Exception("Invalid unicode sequence character");
            }
        }

        return (char) res;
    }

    @Override
    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) throws Exception {

        String s = node.getText();
        int len = s.length();

        StringBuilder buf = new StringBuilder(len);

        for (int i = 1; i < len - 1; i++) {
            char c = s.charAt(i);
            if (c == '\\') {
                ++i;
                char nextC = s.charAt(i);
                switch (nextC) {
                    case 'b':
                        buf.append('\b');
                        break;
                    case 't':
                        buf.append('\t');
                        break;
                    case 'n':
                        buf.append('\n');
                        break;
                    case 'f':
                        buf.append('\f');
                        break;
                    case 'r':
                        buf.append('\r');
                        break;
                    case '"':
                        buf.append('"');
                        break;
                    case '\'':
                        buf.append('\'');
                        break;
                    case '\\':
                        buf.append('\\');
                        break;
                    case 'u':
                        buf.append(processUnicode(s, i + 1));
                        i += 4;
                        break;
                    case '0':
                    case '1':
                    case '2':
                    case '3':
                    case '4':
                    case '5':
                    case '6':
                    case '7':
                        i += processOctal(s, i, buf) - 1;
                        break;
                }
            } else {
                buf.append(c);
            }
        } // end for

        return new LiteralBoundNode(node, buf.toString(), JavaOpenClass.STRING);
    }

}
