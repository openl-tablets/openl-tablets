package org.openl.runtime;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMember;
import org.openl.types.IOpenMethod;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.SimpleVM;

public class OpenLInvocationHandler implements InvocationHandler, IEngineWrapper {

    private Object openlInstance;
    private AEngineFactory engineFactory;
    private Map<Method, IOpenMember> methodMap;

    public OpenLInvocationHandler(Object openlInstance,
            AEngineFactory engineFactory,
            IRuntimeEnv openlEnv,
            Map<Method, IOpenMember> methodMap) {
        this.openlInstance = openlInstance;
        this.engineFactory = engineFactory;
        setRuntimeEnv(openlEnv);
        this.methodMap = methodMap;
    }

    private ThreadLocal<IRuntimeEnv> environment = new ThreadLocal<IRuntimeEnv>() {
        @Override
        protected IRuntimeEnv initialValue() {
            IRuntimeEnv environment = new SimpleVM().getRuntimeEnv();
            return environment;
        }
    };

    public AEngineFactory getFactory() {
        return engineFactory;
    }

    public Object getInstance() {
        return openlInstance;
    }

    public IRuntimeEnv getRuntimeEnv() {
        return environment.get();
    }

    private void setRuntimeEnv(IRuntimeEnv env) {
        if (env != null) {
            environment.set(env);
        } 
    }

    protected Map<Method, IOpenMember> getMethodMap() {
        return methodMap;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        if (method.getDeclaringClass() == IEngineWrapper.class) {
            Method wrapperMethod = OpenLInvocationHandler.class.getDeclaredMethod(method.getName(), new Class<?>[0]);
            return wrapperMethod.invoke(this, args);
        }

        if (ArrayUtils.contains(engineFactory.getInstanceInterfaces(), method.getDeclaringClass())) {

            IOpenMember member = methodMap.get(method);

            if (member instanceof IOpenMethod) {
                IOpenMethod openMethod = (IOpenMethod) member;
                return openMethod.invoke(openlInstance, args, environment.get());
            } else {
                IOpenField openField = (IOpenField) member;
                return openField.get(openlInstance, environment.get());
            }
        } else {

            Class<?>[] cargs = {};

            // TODO: What does this code mean?
            if (args != null && args.length == 1) {
                cargs = new Class<?>[] { Object.class };
            }

            Method objectMethod = Object.class.getDeclaredMethod(method.getName(), cargs);
            return objectMethod.invoke(this, args);
        }
    }

    @Override
    public boolean equals(Object obj) {

        if (obj == null) {
            return false;
        }

        if (obj instanceof Proxy) {
            return Proxy.getInvocationHandler(obj) == this;
        }

        return super.equals(obj);
    }

}
