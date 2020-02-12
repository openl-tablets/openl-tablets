package org.openl.runtime;

import java.lang.reflect.Method;
import java.util.Map;

import org.openl.types.IOpenField;
import org.openl.types.IOpenMember;
import org.openl.types.IOpenMethod;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.SimpleVM;

import javassist.util.proxy.ProxyObject;

public class OpenLMethodHandler implements IOpenLMethodHandler<Method, IOpenMember>, IEngineWrapper {

    private Object openlInstance;
    private Map<Method, IOpenMember> methodMap;

    public OpenLMethodHandler(Object openlInstance, Map<Method, IOpenMember> methodMap) {
        this.openlInstance = openlInstance;
        this.methodMap = methodMap;
    }

    public OpenLMethodHandler(Object openlInstance, IRuntimeEnv openlEnv, Map<Method, IOpenMember> methodMap) {
        this(openlInstance, methodMap);
        setRuntimeEnv(openlEnv);
    }

    @Override
    public IOpenMember getTargetMember(Method key) {
        return methodMap.get(key);
    }

    private ThreadLocal<IRuntimeEnv> env = ThreadLocal.withInitial(this::makeRuntimeEnv);

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
    public Object invoke(Object proxy, Method method, Method proceed, Object[] args) throws Throwable {
        if (IEngineWrapper.class.equals(method.getDeclaringClass())) {
            return method.invoke(this, args);
        }
        if (Object.class.equals(method.getDeclaringClass())) {
            return method.invoke(this, args);
        } else {
            IOpenMember member = methodMap.get(method);
            if (member instanceof IOpenMethod) {
                IOpenMethod openMethod = (IOpenMethod) member;
                return openMethod.invoke(openlInstance, args, getRuntimeEnv());
            } else {
                IOpenField openField = (IOpenField) member;
                return openField.get(openlInstance, getRuntimeEnv());
            }
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj instanceof ProxyObject) {
            return obj.equals(this);
        }
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

}
