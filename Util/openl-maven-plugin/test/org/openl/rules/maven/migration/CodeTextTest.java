package org.openl.rules.maven.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CodeTextTest {

    @Test
    void splitsArgumentsAtTopLevelCommas() {
        assertEquals(List.of("a", "b(c, d)", "\"x, y\"", "{1, 2}", "'\\''", "e[1, 2]"),
                CodeText.arguments("name( a , b(c, d), \"x, y\", {1, 2}, '\\'', e[1, 2])", "name"));
    }

    @Test
    void readsCallWithoutArguments() {
        assertEquals(List.of(), CodeText.arguments("name()", "name"));
        assertEquals(List.of("a /* e, f */"), CodeText.arguments("name (a /* e, f */)", "name"));
        assertEquals(List.of("a", ""), CodeText.arguments("name(a, )", "name"));
        assertEquals(List.of("`a, b`", "c"), CodeText.arguments("name(`a, b`, c)", "name"));
    }

    @Test
    void rejectsTextThatIsNotWholeCallOfFunction() {
        assertNull(CodeText.arguments("other(a)", "name"));
        assertNull(CodeText.arguments("names(a)", "name"));
        assertNull(CodeText.arguments("name", "name"));
        assertNull(CodeText.arguments("name(a)[0]", "name"));
        assertNull(CodeText.arguments("name(a", "name"));
        assertNull(CodeText.arguments("name(a) + name(b)", "name"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"x", " x.y ", "f(a, b)", "a.b(c)[0]", "\"a + b\"", "'+'", "1.5", "$Step", "{1, 2}",
            "(a + b)", "`a b`.c"})
    void singleOperandIsAtomic(String expression) {
        assertTrue(CodeText.isAtomic(expression));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "a + b", "-1", "a == null ? b : c", "new Foo()", "!a", "f(a", "x // comment"})
    void expressionWithOperatorIsNotAtomic(String expression) {
        assertFalse(CodeText.isAtomic(expression));
    }

    @Test
    void findsCallOutsideLiterals() {
        assertTrue(CodeText.hasCall("lookup(x)"));
        assertTrue(CodeText.hasCall("a[f(x)]"));
        assertTrue(CodeText.hasCall("(a + b)"));
        assertFalse(CodeText.hasCall("person.name"));
        assertFalse(CodeText.hasCall("a[0] + \"f(x)\""));
    }

    @Test
    void wholeExpressionIsDelimited() {
        assertDelimited("<x>");
        assertDelimited(" <x> ");
        assertDelimited("f(<x>)");
        assertDelimited("f(a, <x>, b)");
        assertDelimited("a[<x>]");
        assertDelimited("{<x>, 1}");
        assertDelimited("y = <x>;");
        assertDelimited("y += <x>;");
        assertDelimited("return <x>;");
        assertDelimited("return(<x>)");
        assertDelimited("a;\n<x>");
    }

    @Test
    void operandOfOperatorIsNotDelimited() {
        assertNotDelimited("<x>[0]");
        assertNotDelimited("<x>.length");
        assertNotDelimited("a + <x>");
        assertNotDelimited("<x> + a");
        assertNotDelimited("a == <x>");
        assertNotDelimited("a != <x>");
        assertNotDelimited("a <= <x>");
        assertNotDelimited("c ? <x> : b");
        assertNotDelimited("noreturn <x>");
        assertNotDelimited("y = <x> + 1");
    }

    private static void assertDelimited(String template) {
        assertTrue(isDelimited(template), template);
    }

    private static void assertNotDelimited(String template) {
        assertFalse(isDelimited(template), template);
    }

    private static boolean isDelimited(String template) {
        var start = template.indexOf("<x>");
        var code = template.replace("<x>", "x == null ? a : b");
        return CodeText.isDelimited(code, start, start + "x == null ? a : b".length());
    }
}
