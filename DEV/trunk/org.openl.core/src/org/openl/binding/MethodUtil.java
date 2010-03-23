/*
 * Created on Jun 3, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding;

import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.IOpenMethodHeader;
import org.openl.util.print.Formatter;

/**
 * @author snshor
 *
 */
public class MethodUtil {

    static StringBuffer printMethod(IOpenMethod method, StringBuffer buf) {
        return printMethod(method.getName(), method.getSignature(), buf);
    }

    static public String printMethod(IOpenMethodHeader m, int mode, boolean printType) {
        StringBuffer buf = new StringBuffer(100);

        if (printType) {
            buf.append(m.getType().getDisplayName(mode)).append(' ');
        }

        buf.append(m.getName()).append('(');

        IMethodSignature signature = m.getSignature();

        IOpenClass[] params = signature.getParameterTypes();
        for (int i = 0; i < params.length; i++) {
            if (i > 0) {
                buf.append(',');
            }
            buf.append(params[i].getDisplayName(mode));
            if (signature.getParameterName(i) != null) {
                buf.append(' ').append(signature.getParameterName(i));
            }
        }
        buf.append(')');

        return buf.toString();
    }

    static public String printMethod(String name, Class<?>[] params) {
        return printMethod(name, params, new StringBuffer()).toString();
    }

    static public StringBuffer printMethod(String name, Class<?>[] params, StringBuffer buf) {
        buf.append(name).append("(");
        for (int i = 0; i < params.length; i++) {
            if (i > 0) {
                buf.append(',');
            }
            buf.append(params[i].getName());
        }
        buf.append(')');
        return buf;
    }

    static public StringBuffer printMethod(String name, IMethodSignature signature, StringBuffer buf) {
        IOpenClass[] params = signature.getParameterTypes();
        buf.append(name).append("(");
        for (int i = 0; i < params.length; i++) {
            if (i > 0) {
                buf.append(',');
            }
            buf.append(params[i].getName());
            if (signature.getParameterName(i) != null) {
                buf.append(' ').append(signature.getParameterName(i));
            }
        }
        buf.append(')');
        return buf;
    }

    static public String printMethod(String name, IOpenClass[] params) {
        return printMethod(name, params, new StringBuffer()).toString();
    }

    static public StringBuffer printMethod(String name, IOpenClass[] params, StringBuffer buf) {
        buf.append(name).append("(");
        for (int i = 0; i < params.length; i++) {
            if (i > 0) {
                buf.append(',');
            }
            buf.append(params[i].getName());
        }
        buf.append(')');
        return buf;
    }

    
    
    static public StringBuffer printMethodWithParams(IOpenMethod method, Object[] params, int mode, StringBuffer buf) {
//        buf.append(method.getType().getDisplayName(mode)).append(' ');
        buf.append(method.getName()).append('(');

//        IOpenClass[] paramTypes = method.getSignature().getParameterTypes();

        for (int i = 0; i < params.length; i++) {
            if (i > 0) {
                buf.append(", ");
            }
 //           buf.append(paramTypes[i].getDisplayName(mode)).append(' ');
            buf.append(method.getSignature().getParameterName(i)).append(" = ");
            Formatter.format(params[i], mode, buf);
        }

        buf.append(')');
        // buf.append(MethodUtil.printMethod(getDT(), IMetaInfo.REGULAR,
        // false));
        return buf;
    }
    
}
