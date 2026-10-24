package org.openl.rules.maven.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Date;

import org.junit.jupiter.api.Test;

import org.openl.binding.exception.MethodNotFoundException;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.types.IOpenClass;
import org.openl.types.NullOpenClass;
import org.openl.types.java.JavaOpenClass;

class FunctionFixTest {

    private static final CodeFix FORMAT_NUMBER = CodeFix.function("format", "toString({0}, \"#,##0.00\")",
            Number.class);
    private static final CodeFix ADD = CodeFix.function("add", "{1} == null ? {0} : add({0}, {1})",
            Object[].class, Object.class);

    @Test
    void rewritesCallWithArgumentsOfTheTypes() {
        assertEquals("toString(policy.amount, \"#,##0.00\")",
                FORMAT_NUMBER.fix("format(policy.amount)", notFound("format", Double.class)));
        assertEquals("toString(a + b, \"#,##0.00\")",
                FORMAT_NUMBER.fix("format (a + b)", notFound("format", double.class)));
        assertEquals("toString(n, \"#,##0.00\")", FORMAT_NUMBER.fix("format(n)", notFound("format", int.class)));
    }

    @Test
    void keepsCallOfOtherTypesOrNames() {
        assertNull(FORMAT_NUMBER.fix("format(d)", notFound("format", Date.class)));
        assertNull(FORMAT_NUMBER.fix("format(c)", notFound("format", char.class)));
        assertNull(FORMAT_NUMBER.fix("format(x, y)", notFound("format", Double.class, String.class)));
        assertNull(FORMAT_NUMBER.fix("format(null)", new MethodNotFoundException("format", NullOpenClass.the)));
        assertNull(FORMAT_NUMBER.fix("formatted(x)", notFound("formatted", Double.class)));
        assertNull(FORMAT_NUMBER.fix("format(x)", new OpenlNotCheckedException("Method is not found.")));
        assertNull(FORMAT_NUMBER.fix("format(x)", null));
    }

    @Test
    void keepsMethodCalledOnValue() {
        assertNull(FORMAT_NUMBER.fix("value.format(x)", notFound("format", Double.class)));
    }

    @Test
    void parenthesizesArgumentNextToOperator() {
        assertEquals("(a ? x : y) == null ? names : add(names, a ? x : y)",
                ADD.fix("add(names, a ? x : y)", notFound("add", String[].class, String.class)));
        assertEquals("name == null ? (a ? b : c) : add(a ? b : c, name)",
                ADD.fix("add(a ? b : c, name)", notFound("add", String[].class, String.class)));
    }

    @Test
    void keepsCallThatWouldRunTwice() {
        assertNull(ADD.fix("add(names, lookup(x))", notFound("add", String[].class, String.class)));
        assertNull(ADD.fix("add(lookup(x), name)", notFound("add", String[].class, String.class)));
        assertEquals("person.name == null ? names : add(names, person.name)",
                ADD.fix("add(names, person.name)", notFound("add", String[].class, String.class)));
        assertEquals("toString(lookup(x), \"#,##0.00\")",
                FORMAT_NUMBER.fix("format(lookup(x))", notFound("format", Double.class)));
    }

    @Test
    void acceptsSubtypes() {
        assertEquals("x == null ? names : add(names, x)",
                ADD.fix("add(names, x)", notFound("add", String[].class, Integer.class)));
        assertNull(ADD.fix("add(values, x)", notFound("add", int[].class, Integer.class)));
    }

    @Test
    void replacesIdentifier() {
        var fix = CodeFix.identifier("DEFAULT_DOUBLE_FORMAT", "\"#,##0.00\"");
        assertEquals("\"#,##0.00\"", fix.fix("DEFAULT_DOUBLE_FORMAT", null));
        assertNull(fix.fix("DEFAULT_DOUBLE_FORMAT_X", null));
    }

    private static MethodNotFoundException notFound(String name, Class<?>... types) {
        var params = new IOpenClass[types.length];
        for (var i = 0; i < types.length; i++) {
            params[i] = JavaOpenClass.getOpenClass(types[i]);
        }
        return new MethodNotFoundException(name, params);
    }
}
