package org.openl.types.impl;

import org.openl.OpenL;
import org.openl.binding.impl.Binder;
import org.openl.binding.impl.BindingContext;
import org.openl.engine.OpenLManager;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.impl.StringSourceCodeModule;
import org.openl.types.IMethodCaller;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.IRuntimeEnv;

/**
 * @author Marat Kamalov
 *
 */
public class SourceCodeMethodCaller implements IMethodCaller {
    IOpenMethod method;
    String sourceCode;
    IMethodSignature signature;
    IOpenClass resultType;

    public SourceCodeMethodCaller(IMethodSignature signature, IOpenClass resultType, String sourceCode) {
        if (signature == null) {
            throw new IllegalArgumentException("signature can't be null!");
        }
        if (sourceCode == null) {
            throw new IllegalArgumentException("sourceCode can't be null!");
        }
        this.signature = signature;
        this.sourceCode = sourceCode;
        this.resultType = resultType;
    }

    public IOpenMethod getMethod() {
        if (method == null) {
            IOpenSourceCodeModule src = new StringSourceCodeModule(sourceCode, null);
            OpenL op = OpenL.getInstance(OpenL.OPENL_J_NAME);
            OpenMethodHeader methodHeader = new OpenMethodHeader("run",
                resultType == null ? JavaOpenClass.VOID : resultType,
                signature,
                null);
            BindingContext cxt = new BindingContext((Binder) op.getBinder(), null, op);
            method = OpenLManager.makeMethod(op, src, methodHeader, cxt);
        }
        return method;
    }

    public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        return getMethod().invoke(null, params, env);
    }
}
