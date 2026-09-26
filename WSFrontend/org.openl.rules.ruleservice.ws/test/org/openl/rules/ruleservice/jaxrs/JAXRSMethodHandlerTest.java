package org.openl.rules.ruleservice.jaxrs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
        assertNotNull(new JAXRSMethodHandler(new Object(), new HashMap<>()));
    }

    @Test
    void checkNullTargetConstructorArgument() {
        assertThrows(NullPointerException.class, () -> {
            new JAXRSMethodHandler(null, new HashMap<>());
        });
    }

    @Test
    void checkNullMethodsConstructorArgument() {
        var target = new Object();
        assertThrows(NullPointerException.class, () -> {
            new JAXRSMethodHandler(target, null);
        });
    }

    @Test
    void checkInvokeOnUnknownMethod() throws Throwable {
        var target = new Object();
        var methods = new HashMap<Method, Method>();
        var handler = new JAXRSMethodHandler(target, methods);
        var unknownMethod = Object.class.getDeclaredMethod("hashCode");
        assertThrows(IllegalStateException.class, () -> handler.invoke(unknownMethod, null));
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
