package org.openl.runtime;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Map;

import org.openl.types.IOpenField;
import org.openl.types.IOpenMember;
import org.openl.types.IOpenMethod;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.SimpleVM;

public class OpenLMethodHandler implements IOpenLMethodHandler<Method, IOpenMember>, IEngineWrapper {

    private final Object openlInstance;
    private final Map<Method, IOpenMember> methodMap;

    public OpenLMethodHandler(Object openlInstance, Map<Method, IOpenMember> methodMap) {
        this.openlInstance = openlInstance;
        this.methodMap = methodMap;
    }

    public OpenLMethodHandler(Object openlInstance, IRuntimeEnv runtimeEnv, Map<Method, IOpenMember> methodMap) {
        this(openlInstance, methodMap);
        setRuntimeEnv(runtimeEnv);
    }

    @Override
    public IOpenMember getTargetMember(Method key) {
        return methodMap.get(key);
    }

    @Override
    public IOpenMember getOpenMember(Method key) {
        return methodMap.get(key);
    }

    private final ThreadLocal<IRuntimeEnv> env = ThreadLocal.withInitial(this::makeRuntimeEnv);

    public IRuntimeEnv makeRuntimeEnv() {
        return new SimpleVM().getRuntimeEnv();
    }

    @Override
    public Object getInstance() {
        return openlInstance;
    }

    @Override
    public Object getTarget() {
        return getInstance();
    }

    @Override
    public IRuntimeEnv getRuntimeEnv() {
        return env.get();
    }

    public void setRuntimeEnv(IRuntimeEnv runtimeEnv) {
        if (runtimeEnv != null) {
            env.set(runtimeEnv);
        }
    }

    protected Map<Method, IOpenMember> getMethodMap() {
        return methodMap;
    }

    @Override
    public void release() {
        env.remove();
    }

    @Override
    public Object invoke(Method method, Object[] args) throws Exception {
        if (IEngineWrapper.class == method.getDeclaringClass()) {
            return method.invoke(this, args);
        }
        if (Object.class == method.getDeclaringClass()) {
            return method.invoke(this, args);
        } else {
            IOpenMember member = methodMap.get(method);
            if (member instanceof IOpenMethod) {
                IOpenMethod openMethod = (IOpenMethod) member;
                Object ret = openMethod.invoke(openlInstance, args, getRuntimeEnv());
                if (method.getReturnType() != void.class && openMethod.getType() == JavaOpenClass.VOID || openMethod
                    .getType() == JavaOpenClass.CLS_VOID && method.getReturnType().isPrimitive()) {
                    return Array.get(Array.newInstance(method.getReturnType(), 1), 0);
                }
                return ret;
            } else {
                IOpenField openField = (IOpenField) member;
                Object ret = openField.get(openlInstance, getRuntimeEnv());
                if (method.getReturnType() != void.class && openField.getType() == JavaOpenClass.VOID || openField.getType() == JavaOpenClass.CLS_VOID && method
                    .getReturnType()
                    .isPrimitive()) {
                    return Array.get(Array.newInstance(method.getReturnType(), 1), 0);
                }
                return ret;
            }
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (ASMProxyFactory.isProxy(obj)) {
            return obj.equals(this);
        }
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

}
