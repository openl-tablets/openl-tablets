package org.openl.runtime;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;

import org.openl.types.IOpenField;
import org.openl.types.IOpenMember;
import org.openl.types.IOpenMethod;
import org.openl.vm.IRuntimeEnv;

public class OpenLInvocationHandler<T> implements InvocationHandler, IEngineWrapper<T> {

    private Object openlInstance;
    private EngineFactory<T> engineFactory;
    private IRuntimeEnv openlEnv;
    private Map<Method, IOpenMember> methodMap;

    public OpenLInvocationHandler(Object openlInstance, EngineFactory<T> engineFactory, IRuntimeEnv openlEnv, Map<Method, IOpenMember> methodMap) {
        this.openlInstance = openlInstance;
        this.engineFactory = engineFactory;
        this.openlEnv = openlEnv;
        this.methodMap = methodMap;
    }

    public EngineFactory<T> getFactory() {
        return engineFactory;
    }

    public Object getInstance() {
        return openlInstance;
    }

    public IRuntimeEnv getRuntimeEnv() {
        return openlEnv;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        
        Object executionResult;

        if (method.getDeclaringClass() == engineFactory.getEngineInterface()) {

            IOpenMember member = methodMap.get(method);

            if (member instanceof IOpenMethod) {
                IOpenMethod openMethod = (IOpenMethod) member;
                executionResult = openMethod.invoke(openlInstance, args, openlEnv);
            } else {
                IOpenField openField = (IOpenField) member;
                executionResult = openField.get(openlInstance, openlEnv);
            }
        } else {

            Class<?>[] cargs = {};

            // TODO: What does this code mean?
            if (args != null && args.length == 1) {
                cargs = new Class<?>[] { Object.class };
            }

            if (method.getDeclaringClass() == IEngineWrapper.class) {
                Method myMethod = OpenLInvocationHandler.class.getDeclaredMethod(method.getName(), cargs);
                executionResult = myMethod.invoke(this, args);
            } else {
                Method objectMethod = Object.class.getDeclaredMethod(method.getName(), cargs);
                executionResult = objectMethod.invoke(this, args);
            }
        }

        return executionResult;
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

    @Override
    public String toString() {
        return String.format("Rule Engine(%s)", engineFactory.getOpenClass().getName());
    }

}
