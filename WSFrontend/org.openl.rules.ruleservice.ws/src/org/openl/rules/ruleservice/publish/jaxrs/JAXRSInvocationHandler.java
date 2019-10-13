package org.openl.rules.ruleservice.publish.jaxrs;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;

import javax.ws.rs.core.Response;

import org.openl.runtime.IOpenLInvocationHandler;

public class JAXRSInvocationHandler implements IOpenLInvocationHandler<Method, Method> {

    private Object target;
    private Map<Method, Method> methodMap;

    @Override
    public Method getTargetMember(Method key) {
        return methodMap.get(key);
    }

    public JAXRSInvocationHandler(Object target, Map<Method, Method> methodMap) {
        this.target = Objects.requireNonNull(target, "target can't be null.");
        this.methodMap = Objects.requireNonNull(methodMap, "methodMap can't be null.");
    }

    @Override
    public Object getTarget() {
        return target;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Method m = methodMap.get(method);
        if (m == null) {
            throw new IllegalStateException("Method is not found in the map of methods.");
        }
        if (args != null && args.length == 1) {
            int targetParamCount = m.getParameterTypes().length;
            if (targetParamCount > 1) {
                Object requestObject = args[0];
                if (requestObject == null) {
                    args = new Object[targetParamCount];
                } else {
                    args = (Object[]) requestObject.getClass().getMethod("_args").invoke(requestObject);
                }
            }
        }

        Object o = m.invoke(target, args);
        if (o instanceof Response) {
            return o;
        } else {
            return Response.status(Response.Status.OK).entity(o).build();
        }
    }
}