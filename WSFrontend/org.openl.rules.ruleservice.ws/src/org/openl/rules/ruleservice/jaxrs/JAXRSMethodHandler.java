package org.openl.rules.ruleservice.jaxrs;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Map;
import java.util.Objects;
import jakarta.ws.rs.core.Response;

import org.openl.rules.ruleservice.publish.jaxrs.JAXRSOpenLServiceEnhancerHelper;
import org.openl.runtime.AbstractOpenLMethodHandler;

class JAXRSMethodHandler extends AbstractOpenLMethodHandler<Method, Method> {

    private final Object target;
    private final Map<Method, Method> methodMap;

    @Override
    public Method getTargetMember(Method key) {
        return methodMap.get(key);
    }

    public JAXRSMethodHandler(Object target, Map<Method, Method> methodMap) {
        this.target = Objects.requireNonNull(target, "target cannot be null");
        this.methodMap = Objects.requireNonNull(methodMap, "methodMap cannot be null");
    }

    @Override
    public Object getTarget() {
        return target;
    }

    @Override
    public Object invoke(Method method, Object[] args) throws Exception {
        Method m = methodMap.get(method);
        if (m == null) {
            throw new IllegalStateException("Method is not found in the map of methods.");
        }
        if (args != null && args.length > 0) {
            if (method.getParameterCount() != m.getParameterCount()) {
                Object requestObject = args[0];
                Object[] newArgs = new Object[m.getParameterCount()];
                Object[] requestWrapperArgs = null;
                if (requestObject != null) {
                    requestWrapperArgs = (Object[]) requestObject.getClass().getMethod("_args").invoke(requestObject);
                }
                int i = 0;
                int j = 1;
                int k = 0;
                for (Parameter parameter : m.getParameters()) {
                    if (JAXRSOpenLServiceEnhancerHelper.isParameterInWrapperClass(parameter)) {
                        newArgs[i] = requestWrapperArgs != null ? requestWrapperArgs[k++] : null;
                    } else {
                        newArgs[i] = args[j++];
                    }
                    i++;
                }
                args = newArgs;
            }
        }

        Object o;

        try {
            o = m.invoke(target, args);
        } catch (InvocationTargetException | UndeclaredThrowableException e) {
            Throwable ex = e.getCause();
            throw ex instanceof Exception ? (Exception) ex : e;
        }

        if (o instanceof Response) {
            return o;
        } else {
            return Response.status(o == null ? Response.Status.NO_CONTENT : Response.Status.OK).entity(o).build();
        }
    }
}