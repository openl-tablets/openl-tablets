package org.openl.rules.ruleservice.jaxrs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.HashMap;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.Test;

class JAXRSMethodHandlerTest {

    @Test
    void checkNotNullConstructorArguments() {
        new JAXRSMethodHandler(new Object(), new HashMap<>());
    }

    @Test
    void checkNullTargetConstructorArgument() {
        assertThrows(NullPointerException.class, () -> {
            new JAXRSMethodHandler(null, new HashMap<>());
        });
    }

    @Test
    void checkNullMethodsConstructorArgument() {
        assertThrows(NullPointerException.class, () -> {
            new JAXRSMethodHandler(new Object(), null);
        });
    }

    @Test
    void checkInvokeOnUnknownMethod() throws Throwable {
        assertThrows(IllegalStateException.class, () -> {
            var target = new Object();
            var methods = new HashMap<Method, Method>();
            var handler = new JAXRSMethodHandler(target, methods);
            var unknownMethod = Object.class.getDeclaredMethod("hashCode");
            handler.invoke(unknownMethod, null);
        });
    }

    @Test
    void checkNullArguments() throws Throwable {
        InvokedClass target = mock(InvokedClass.class);
        when(target.doWork()).thenReturn("Done");
        var methods = new HashMap<Method, Method>();
        var method = target.getClass().getDeclaredMethod("doWork");
        methods.put(method, method);

        var handler = new JAXRSMethodHandler(target, methods);
        var result = handler.invoke(method, null);

        assertTrue(result instanceof Response);
        assertEquals("Done", ((Response) result).getEntity());
    }

    private interface InvokedClass {
        String doWork();
    }
}
