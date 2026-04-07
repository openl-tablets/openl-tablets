package org.openl.rules.webstudio.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class ProjectBeanConvertRegexToGlobTest {

    @Test
    void testMatchAll() {
        assertEquals("*", ProjectBean.convertRegexToGlob(".*"));
        assertEquals("*", ProjectBean.convertRegexToGlob(".+"));
    }

    @Test
    void testNullAndBlank() {
        assertNull(ProjectBean.convertRegexToGlob(null));
        assertNull(ProjectBean.convertRegexToGlob(""));
        assertNull(ProjectBean.convertRegexToGlob("  "));
    }

    @Test
    void testInvalidRegex() {
        // "*" is not valid regex — should be ignored
        assertNull(ProjectBean.convertRegexToGlob("*"));
        // Unbalanced brackets
        assertNull(ProjectBean.convertRegexToGlob("[abc"));
    }

    @Test
    void testRegexThatCannotMatchSignature() {
        // Valid regex but cannot match any method signature like "Type name(Args)"
        assertNull(ProjectBean.convertRegexToGlob("^\\d+$"));
        assertNull(ProjectBean.convertRegexToGlob("^$"));
    }

    @Test
    void testSignatureWithEscapedParens() {
        // ".+ methodName\(.+\)" - method with any return type and at least one param
        assertEquals("SayHello", ProjectBean.convertRegexToGlob(".+ SayHello\\(.+\\)"));

        // ".+ methodName\(.*\)" - method with any return type and optional params
        assertEquals("main1", ProjectBean.convertRegexToGlob(".+ main1\\(.*\\)"));
        assertEquals("main2", ProjectBean.convertRegexToGlob(".* main2\\(.*\\)"));

        // ".+ methodName\(\)" - method with no params
        assertEquals("mySpr", ProjectBean.convertRegexToGlob(".+ mySpr\\(\\)"));
    }

    @Test
    void testSignatureWithUnescapedParens() {
        // ".* getFactor(.*)" - unescaped parens
        assertEquals("getFactor*", ProjectBean.convertRegexToGlob(".* getFactor(.*)"));
        assertEquals("SayHello*", ProjectBean.convertRegexToGlob(".+ SayHello(.+)"));
        assertEquals("Say?Hello*", ProjectBean.convertRegexToGlob(".+ Say.Hello(.+)"));
    }

    @Test
    void testWrappedWildcards() {
        // ".*methodName.*" - method name surrounded by wildcards → exact name
        assertEquals("*method2*", ProjectBean.convertRegexToGlob(".*method2.*"));

        // ".+methodName.+" - same but with .+
        assertEquals("*Ping*", ProjectBean.convertRegexToGlob(".+Ping.+"));
    }

    @Test
    void testComplexRegexIgnored() {
        // Regex patterns that match signatures but cannot be cleanly converted to glob — ignored
        assertNull(ProjectBean.convertRegexToGlob("[a-z]+Method"));
        assertNull(ProjectBean.convertRegexToGlob("(foo|bar)"));
        assertNull(ProjectBean.convertRegexToGlob(".* (foo|bar).*"));
        assertNull(ProjectBean.convertRegexToGlob(".* foo\\..*"));
        assertNull(ProjectBean.convertRegexToGlob(".* foob?.*"));
        assertNull(ProjectBean.convertRegexToGlob(".* fo+.*"));
        assertNull(ProjectBean.convertRegexToGlob(".* foo[b].*"));
        assertNull(ProjectBean.convertRegexToGlob(".* fo{2}.*"));
        assertNull(ProjectBean.convertRegexToGlob(".* f\\o.*"));
        assertNull(ProjectBean.convertRegexToGlob(".* f o.*"));
    }

    @Test
    void testFullQualified() {
        // Regex patterns that match signatures but cannot be cleanly converted to glob — ignored
        assertEquals("getTest*", ProjectBean.convertRegexToGlob("void getTest(.*)"));
        assertEquals("getTest*", ProjectBean.convertRegexToGlob("java.lang.String getTest(.*)"));
        assertEquals("getTest", ProjectBean.convertRegexToGlob("java\\.lang\\.String getTest\\(.*\\)"));
        assertEquals("getTest", ProjectBean.convertRegexToGlob("java\\.lang\\.String getTest\\(int, long\\)"));
    }

    @Test
    void testSimpleNameIgnored() {
        // A plain method name like "myMethod" is not a valid method-filter regexp
        // because it cannot match any signature "returnType name(args)"
        assertNull(ProjectBean.convertRegexToGlob("myMethod"));
    }
}
