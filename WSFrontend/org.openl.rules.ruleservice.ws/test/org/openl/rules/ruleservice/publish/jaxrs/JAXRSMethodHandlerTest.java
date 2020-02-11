package org.openl.rules.ruleservice.publish.jaxrs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.HashMap;

import javax.ws.rs.core.Response;

import org.junit.Test;

public class JAXRSMethodHandlerTest {

    @Test
    public void checkNotNullConstructorArguments() {
        new JAXRSMethodHandler(new Object(), new HashMap<Method, Method>());
    }

    @Test(expected = NullPointerException.class)
    public void checkNullTargetConstructorArgument() {
        new JAXRSMethodHandler(null, new HashMap<Method, Method>());
    }

    @Test(expected = NullPointerException.class)
    public void checkNullMethodsConstructorArgument() {
        new JAXRSMethodHandler(new Object(), null);
    }

    @Test(expected = IllegalStateException.class)
    public void checkInvokeOnUnknownMethod() throws Throwable {
        Object target = new Object();
        HashMap<Method, Method> methods = new HashMap<>();
        JAXRSMethodHandler handler = new JAXRSMethodHandler(target, methods);
        Method unknownMethod = Object.class.getDeclaredMethod("hashCode");
        handler.invoke(null, unknownMethod, null, null);
    }

    @Test
    public void checkNullArguments() throws Throwable {
        InvokedClass target = mock(InvokedClass.class);
        when(target.doWork()).thenReturn("Done");
        HashMap<Method, Method> methods = new HashMap<>();
        Method method = target.getClass().getDeclaredMethod("doWork");
        methods.put(method, method);

        JAXRSMethodHandler handler = new JAXRSMethodHandler(target, methods);
        Object result = handler.invoke(null, method, null, null);

        assertTrue(result instanceof Response);
        assertEquals("Done", ((Response) result).getEntity());
    }

    private interface InvokedClass {
        String doWork();
    }
}
