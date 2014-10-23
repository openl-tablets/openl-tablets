package org.openl.rules.ruleservice.publish.jaxrs;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public class JAXRSInvocationHandler implements InvocationHandler {

    private Object target;
    private Map<Method, Method> methodMap;

    public JAXRSInvocationHandler(Object target, Map<Method, Method> methodMap) {
        if (target == null) {
            throw new IllegalArgumentException("target argument can't be null!");
        }
        if (methodMap == null) {
            throw new IllegalArgumentException("methodMap argument can't be null!");
        }
        this.target = target;
        this.methodMap = methodMap;
    }

    protected String buildErrorMessage(Throwable ex) {
        StringBuilder sb = new StringBuilder();

        sb.append("Exception has been caught");

        Throwable cause = ex.getCause();
        String message = cause == null ? ex.getMessage() : cause.getMessage();
        if (message == null && cause != null) {
            message = "exception cause class: " + cause.getClass().getName();
        }
        if (message != null) {
            sb.append(", message: ").append(message);
        }

        StringWriter errors = new StringWriter();
        ex.printStackTrace(new PrintWriter(errors));
        String stackTraceError = errors.toString();
        if (stackTraceError != null) {
            sb.append(System.getProperty("line.separator"));
            sb.append("Stacktrace: ");
            sb.append(stackTraceError);
        }
        return sb.toString();
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Method m = methodMap.get(method);
        if (m == null) {
            throw new IllegalStateException("Method not found in methods map!");
        }
        try {
            Object o = m.invoke(target, args);
            if (o instanceof Response) {
                return o;
            } else {
                return Response.status(Response.Status.OK).entity(o).build();
            }
        } catch (InvocationTargetException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(buildErrorMessage(e.getTargetException()))
                .type(MediaType.TEXT_PLAIN_TYPE)
                .build();
        }
    }
}