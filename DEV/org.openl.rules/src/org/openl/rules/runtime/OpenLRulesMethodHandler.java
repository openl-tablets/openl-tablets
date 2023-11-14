package org.openl.rules.runtime;

import java.lang.reflect.Method;
import java.util.Map;

import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.context.IRulesRuntimeContextProvider;
import org.openl.runtime.IEngineWrapper;
import org.openl.runtime.IRuntimeEnvBuilder;
import org.openl.runtime.OpenLMethodHandler;
import org.openl.types.IOpenMember;
import org.openl.types.IOpenMethod;

public class OpenLRulesMethodHandler extends OpenLMethodHandler implements IRulesRuntimeContextProvider {

    private final ValidationHandler validationHandler = new ValidationHandler();

    @Override
    public IRulesRuntimeContext getRuntimeContext() {
        return (IRulesRuntimeContext) getRuntimeEnv().getContext();
    }

    public OpenLRulesMethodHandler(Object openlInstance,
            Map<Method, IOpenMember> methodMap,
            IRuntimeEnvBuilder runtimeEnvBuilder) {
        super(openlInstance, methodMap, runtimeEnvBuilder);
    }

    @Override
    public Object invoke(Method method, Object[] args) throws Exception {
        if (IRulesRuntimeContextProvider.class == method.getDeclaringClass()) {
            return method.invoke(this, args);
        }
        if (IEngineWrapper.class != method.getDeclaringClass()) {
            IOpenMember targetMethod = getMethodMap().get(method);
            StringBuilder output = null;
            if (LoggingHandler.isEnabled()) {
                output = new StringBuilder();
                output.append("Method: ").append(targetMethod.getDisplayName(0));
                output.append("\nRuntime Context: ").append(LoggingHandler.convert(getRuntimeContext()));
                if (args.length == 1) {
                    output.append("\nArgs: ").append(LoggingHandler.convert(args[0]));
                } else if (args.length > 1) {
                    output.append("\nArgs: {");
                    for (int i = 0; i < args.length; i++) {
                        output.append('"')
                            .append(((IOpenMethod) targetMethod).getSignature().getParameterName(i))
                            .append("\":");
                        output.append(LoggingHandler.convert(args[i]));
                        if (i < (args.length - 1)) {
                            output.append(',');
                        }
                    }
                    output.append('}');
                }

            }
            Object result = null;
            Exception exception = null;
            try {
                if (targetMethod instanceof IOpenMethod) {
                    validationHandler
                        .validateProxyArguments(((IOpenMethod) targetMethod).getSignature(), getRuntimeEnv(), args);
                }
                result = super.invoke(method, args);
            } catch (Exception e) {
                exception = e;
            }
            if (output != null) {
                if (exception == null) {
                    output.append("\nResult: ").append(LoggingHandler.convert(result));
                } else {
                    output.append("\nException: ").append(LoggingHandler.convert(exception));
                }
                LoggingHandler.log(output);
            }

            if (exception != null) {
                throw exception;
            }
            return result;
        }
        return super.invoke(method, args);
    }
}
