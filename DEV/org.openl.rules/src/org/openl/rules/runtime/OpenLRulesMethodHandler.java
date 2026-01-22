package org.openl.rules.runtime;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;

import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.runtime.IEngineWrapper;
import org.openl.runtime.IOpenLMethodHandler;
import org.openl.runtime.IRuntimeEnvBuilder;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMember;
import org.openl.types.IOpenMethod;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.IRuntimeEnv;

public class OpenLRulesMethodHandler implements IOpenLMethodHandler<Method, IOpenMember>, IEngineWrapper {

    private static final Object[] NO_PARAMS = new Object[0];
    private final ValidationHandler validationHandler = new ValidationHandler();
    private final Object openlInstance;
    private final Map<Method, IOpenMember> methodMap;
    private final IRuntimeEnvBuilder runtimeEnvBuilder;

    public OpenLRulesMethodHandler(Object openlInstance,
                                   Map<Method, IOpenMember> methodMap,
                                   IRuntimeEnvBuilder runtimeEnvBuilder) {
        this.openlInstance = openlInstance;
        this.methodMap = methodMap;
        this.runtimeEnvBuilder = runtimeEnvBuilder;
    }

    @Override
    public Object invoke(Method method, Object[] args) throws Exception {
        if (IEngineWrapper.class == method.getDeclaringClass()) {
            return method.invoke(this, args);
        }
        if (Object.class == method.getDeclaringClass()) {
            return method.invoke(openlInstance, args);
        }

        var member = methodMap.get(method);
        var env = getRuntimeEnv();
        if (args.length > 0) {
            var v = args[0];
            if (v instanceof IRulesRuntimeContext || v == null &&
                    (member instanceof IOpenMethod m && m.getSignature().getParameterTypes().length < args.length
                            || member instanceof IOpenField && args.length == 1
                    )
            ) {
                args = args.length > 1 ? Arrays.copyOfRange(args, 1, args.length) : NO_PARAMS;
                if (v != null) {
                    env.setContext((IRulesRuntimeContext) v);
                }
            }
        }

        StringBuilder output = null;
        if (LoggingHandler.isEnabled()) {
            output = new StringBuilder();
            var sourceClass = member.getDeclaringClass();
            if (sourceClass instanceof XlsModuleOpenClass) {
                output.append("\tModule Name: ").append(((XlsModuleOpenClass) sourceClass).getModuleName())
                        .append('\n');
            }
            output.append("\tMethod: ").append(member.getDisplayName(0));
            output.append("\n\tRuntime Context: ").append(LoggingHandler.convert(env.getContext()));
            if (args.length == 1) {
                output.append("\nArgs: ").append(LoggingHandler.convert(args[0]));
            } else if (args.length > 1) {
                output.append("\n\tArgs: {");
                for (int i = 0; i < args.length; i++) {
                    output.append('"')
                            .append(((IOpenMethod) member).getSignature().getParameterName(i))
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
            if (member instanceof IOpenMethod m) {
                validationHandler.validateProxyArguments(m.getSignature(), env, args);
                result = m.invoke(openlInstance, args, env);
            } else {
                result = ((IOpenField) member).get(openlInstance, env);
            }
            if (method.getReturnType() != void.class && member.getType() == JavaOpenClass.VOID || member
                    .getType() == JavaOpenClass.CLS_VOID && method.getReturnType().isPrimitive()) {
                result = Array.get(Array.newInstance(method.getReturnType(), 1), 0);
            }
        } catch (Exception e) {
            exception = e;
        }
        if (output != null) {
            if (exception == null) {
                output.append("\n\tResult: ").append(LoggingHandler.convert(result));
            } else {
                output.append("\n\tException: ").append(LoggingHandler.convert(exception));
            }
            LoggingHandler.log(output);
        }

        if (exception != null) {
            throw exception;
        }
        return result;
    }

    @Override
    public IOpenMember getTargetMember(Method key) {
        return methodMap.get(key);
    }

    @Override
    public IOpenMember getOpenMember(Method key) {
        return methodMap.get(key);
    }

    @Override
    public Object getTarget() {
        return openlInstance;
    }

    private final ThreadLocal<IRuntimeEnv> env = new ThreadLocal<>();


    @Override
    public Object getInstance() {
        return openlInstance;
    }

    @Override
    public IRuntimeEnv getRuntimeEnv() {
        IRuntimeEnv runtimeEnv = env.get();
        if (runtimeEnv == null) {
            IRuntimeEnv x = runtimeEnvBuilder.buildRuntimeEnv();
            env.set(x);
            return x;
        }
        return runtimeEnv;
    }

    @Override
    public void release() {
        env.remove();
    }
}
