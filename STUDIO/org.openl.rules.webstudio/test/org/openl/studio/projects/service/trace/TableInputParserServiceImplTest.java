package org.openl.studio.projects.service.trace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import org.openl.types.IMethodSignature;
import org.openl.types.IOpenMethod;
import org.openl.types.java.JavaOpenClass;

class TableInputParserServiceImplTest {

    public static class Policy {
        public String policyID;
        public int amount;
    }

    private final TableInputParserServiceImpl parser = new TableInputParserServiceImpl();
    private final ObjectMapper mapper = new ObjectMapper();

    private static IOpenMethod method(String[] names, Class<?>[] types) {
        var signature = mock(IMethodSignature.class);
        when(signature.getNumberOfParameters()).thenReturn(names.length);
        for (int i = 0; i < names.length; i++) {
            when(signature.getParameterName(i)).thenReturn(names[i]);
            when(signature.getParameterType(i)).thenReturn(JavaOpenClass.getOpenClass(types[i]));
        }
        var method = mock(IOpenMethod.class);
        when(method.getSignature()).thenReturn(signature);
        return method;
    }

    @Test
    void singleParamNameWrappedFormFillsTheParameterByName() {
        // The bug: {"policy": {...}} must fill the policy parameter. Previously the whole wrapper was
        // deserialized as Policy (which has no "policy" field) -> an empty object.
        var method = method(new String[]{"policy"}, new Class<?>[]{Policy.class});

        var result = parser.parseInput("{\"policy\":{\"policyID\":\"F900\",\"amount\":5}}", method, mapper);

        var policy = (Policy) result.params()[0];
        assertNotNull(policy);
        assertEquals("F900", policy.policyID);
        assertEquals(5, policy.amount);
        assertNull(result.runtimeContext());
    }

    @Test
    void singleParamRawFormTakesTheWholeJson() {
        // The raw form sends the value's own fields directly (no {"policy": ...} wrapper); the whole
        // JSON is the parameter's value. Must keep working alongside the name-wrapped form above.
        var method = method(new String[]{"policy"}, new Class<?>[]{Policy.class});

        var result = parser.parseInput("{\"policyID\":\"F900\",\"amount\":5}", method, mapper);

        var policy = (Policy) result.params()[0];
        assertNotNull(policy);
        assertEquals("F900", policy.policyID);
        assertEquals(5, policy.amount);
    }

    @Test
    void multipleParamsAreMatchedByName() {
        var method = method(new String[]{"current", "previous"}, new Class<?>[]{Policy.class, Policy.class});

        var result = parser.parseInput(
                "{\"current\":{\"policyID\":\"A\"},\"previous\":{\"policyID\":\"B\"}}", method, mapper);

        assertEquals("A", ((Policy) result.params()[0]).policyID);
        assertEquals("B", ((Policy) result.params()[1]).policyID);
    }

    @Test
    void structuredFormatFillsParamsByName() {
        var method = method(new String[]{"policy"}, new Class<?>[]{Policy.class});

        var result = parser.parseInput("{\"params\":{\"policy\":{\"policyID\":\"F900\"}}}", method, mapper);

        assertEquals("F900", ((Policy) result.params()[0]).policyID);
    }

    @Test
    void blankInputReturnsEmptyParams() {
        var method = method(new String[]{"policy"}, new Class<?>[]{Policy.class});

        var result = parser.parseInput("  ", method, mapper);

        assertEquals(1, result.params().length);
        assertNull(result.params()[0]);
        assertNull(result.runtimeContext());
    }
}
