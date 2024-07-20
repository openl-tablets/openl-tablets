package org.openl.rules.ruleservice.jaxrs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.HashMap;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Test;

public class JAXRSMethodHandlerTest {

    @Test
    public void checkNotNullConstructorArguments() {
        new JAXRSMethodHandler(new Object(), new HashMap<>());
    }

    @Test
    public void checkNullTargetConstructorArgument() {
        assertThrows(NullPointerException.class, () -> {
            new JAXRSMethodHandler(null, new HashMap<>());
        });
    }

    @Test
    public void checkNullMethodsConstructorArgument() {
        assertThrows(NullPointerException.class, () -> {
            new JAXRSMethodHandler(new Object(), null);
        });
    }

    @Test
    public void checkInvokeOnUnknownMethod() throws Throwable {
        assertThrows(IllegalStateException.class, () -> {
            Object target = new Object();
            HashMap<Method, Method> methods = new HashMap<>();
            JAXRSMethodHandler handler = new JAXRSMethodHandler(target, methods);
            Method unknownMethod = Object.class.getDeclaredMethod("hashCode");
            handler.invoke(unknownMethod, null);
        });
    }

    @Test
    public void checkNullArguments() throws Throwable {
        InvokedClass target = mock(InvokedClass.class);
        when(target.doWork()).thenReturn("Done");
        HashMap<Method, Method> methods = new HashMap<>();
        Method method = target.getClass().getDeclaredMethod("doWork");
        methods.put(method, method);

        JAXRSMethodHandler handler = new JAXRSMethodHandler(target, methods);
        Object result = handler.invoke(method, null);

        assertTrue(result instanceof Response);
        assertEquals("Done", ((Response) result).getEntity());
    }

    private interface InvokedClass {
        String doWork();
    }
}
