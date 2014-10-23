/**
 * Created Jul 21, 2007
 */
package org.openl.types.impl;

import java.util.StringTokenizer;

import org.openl.OpenL;
import org.openl.binding.impl.Binder;
import org.openl.binding.impl.BindingContext;
import org.openl.engine.OpenLManager;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.impl.StringSourceCodeModule;
import org.openl.types.IMethodCaller;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.IRuntimeEnv;

public class ParameterMethodCaller implements IMethodCaller {
    int paramNumber;
    IOpenMethod method;
    String code;
    IOpenMethod parameterFieldAccessChainMethod = null;
    boolean noFieldAccessChain = false;

    public ParameterMethodCaller(IOpenMethod method, int paramNumber, String code) {
        this.method = method;
        this.paramNumber = paramNumber;
        this.code = code;
    }

    public IOpenMethod getMethod() {
        return method;
    }

    public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        Object o = params[paramNumber];
        if (noFieldAccessChain) {
            return o;
        }
        if (parameterFieldAccessChainMethod == null) {
            IOpenSourceCodeModule src = new StringSourceCodeModule(code, null);
            OpenL op = OpenL.getInstance(OpenL.OPENL_J_NAME);
            JavaOpenClass openClass = JavaOpenClass.getOpenClass(method.getSignature()
                .getParameterType(paramNumber)
                .getInstanceClass());
            String v = code;
            if (v.indexOf(".") > 0) {
                v = v.substring(0, v.indexOf("."));
            } else {
                noFieldAccessChain = true;
                return o;
            }
            IOpenClass type = getReturnType();
            IMethodSignature signature = new MethodSignature(new IOpenClass[] { openClass }, new String[] { v });
            OpenMethodHeader methodHeader = new OpenMethodHeader("accessToFields",
                type,
                signature,
                null);
            BindingContext cxt = new BindingContext((Binder) op.getBinder(), null, op);
            parameterFieldAccessChainMethod = OpenLManager.makeMethod(op, src, methodHeader, cxt);
        }
        return parameterFieldAccessChainMethod.invoke(null, new Object[] { o }, env);
    }

    private IOpenClass getReturnType() {
        IOpenClass type = method.getSignature().getParameterType(paramNumber);
        StringTokenizer stringTokenizer = new StringTokenizer(code, ".");
        boolean isFirst = true;
        while (stringTokenizer.hasMoreTokens()) {
            String s = stringTokenizer.nextToken();
            if (isFirst) {
                isFirst = false;
                continue;
            }
            boolean arrayAccess = s.matches(".+\\[[0-9]+\\]$");
            IOpenField field = null;
            if (arrayAccess) {
                s = s.substring(0, s.indexOf("["));
            }
            field = type.getField(s);
            type = field.getType();
            if (type.isArray() && arrayAccess) {
                type = type.getComponentClass();
            }
        }
        return type;
    }
}
