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
 */
public class StringNodeBinder extends ANodeBinder {

    public static char processOctal(String s, int startIndex) {
        var len = calcOctalLen(s, startIndex);
        return processCharacter(s, startIndex, len, 8);
    }

    private static int calcOctalLen(String s, int startIndex) {
        var i = startIndex;
        var len = 0;
        while (i < s.length() && len < 3 && validOctal(s.charAt(i))) {
            i++;
            len++;
        }
        if (len < 1) {
            throw new IllegalArgumentException("Invalid character sequence.");
        }
        return len;
    }

    private static boolean validOctal(char ch) {
        return ch >= '0' && ch <= '7';
    }

    public static char processUnicode(String s, int startIndex) {
        return processCharacter(s, startIndex, 4, 16);
    }

    private static char processCharacter(String s, int startIndex, int len, int radix) {
        var endIndex = startIndex + len;
        if (startIndex > endIndex) {
            throw new IllegalArgumentException("Invalid character sequence.");
        }
        return (char) Integer.parseUnsignedInt(s, startIndex, endIndex, radix);
    }

    @Override
    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) {

        var s = node.getText();
        var len = s.length();

        var buf = new StringBuilder(len);

        for (var i = 1; i < len - 1; i++) {
            var c = s.charAt(i);
            if (c == '\\') {
                ++i;
                var nextC = s.charAt(i);
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
                        buf.append(processOctal(s, i));
                        i += calcOctalLen(s, i) - 1;
                        break;
                }
            } else {
                buf.append(c);
            }
        } // end for

        return new LiteralBoundNode(node, buf.toString(), JavaOpenClass.STRING);
    }

}
