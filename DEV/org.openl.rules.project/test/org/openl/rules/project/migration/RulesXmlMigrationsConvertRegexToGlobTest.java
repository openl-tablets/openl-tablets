package org.openl.rules.project.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class RulesXmlMigrationsConvertRegexToGlobTest {

    @Test
    void testMatchAll() {
        assertEquals("*", RulesXmlMigrations.convertRegexToGlob(".*"));
        assertEquals("*", RulesXmlMigrations.convertRegexToGlob(".+"));
    }

    @Test
    void testNullAndBlank() {
        assertNull(RulesXmlMigrations.convertRegexToGlob(null));
        assertNull(RulesXmlMigrations.convertRegexToGlob(""));
        assertNull(RulesXmlMigrations.convertRegexToGlob("  "));
    }

    @Test
    void testInvalidRegex() {
        // "*" is not valid regex — should be ignored
        assertNull(RulesXmlMigrations.convertRegexToGlob("*"));
        // Unbalanced brackets
        assertNull(RulesXmlMigrations.convertRegexToGlob("[abc"));
    }

    @Test
    void testRegexThatCannotMatchSignature() {
        // Valid regex but cannot match any method signature like "Type name(Args)"
        assertNull(RulesXmlMigrations.convertRegexToGlob("^\\d+$"));
        assertNull(RulesXmlMigrations.convertRegexToGlob("^$"));
    }

    @Test
    void testSignatureWithEscapedParens() {
        // ".+ methodName\(.+\)" - method with any return type and at least one param
        assertEquals("SayHello", RulesXmlMigrations.convertRegexToGlob(".+ SayHello\\(.+\\)"));

        // ".+ methodName\(.*\)" - method with any return type and optional params
        assertEquals("main1", RulesXmlMigrations.convertRegexToGlob(".+ main1\\(.*\\)"));
        assertEquals("main2", RulesXmlMigrations.convertRegexToGlob(".* main2\\(.*\\)"));

        // ".+ methodName\(\)" - method with no params
        assertEquals("mySpr", RulesXmlMigrations.convertRegexToGlob(".+ mySpr\\(\\)"));
    }

    @Test
    void testSignatureWithUnescapedParens() {
        // ".* getFactor(.*)" - unescaped parens
        assertEquals("getFactor*", RulesXmlMigrations.convertRegexToGlob(".* getFactor(.*)"));
        assertEquals("SayHello*", RulesXmlMigrations.convertRegexToGlob(".+ SayHello(.+)"));
        assertEquals("Say?Hello*", RulesXmlMigrations.convertRegexToGlob(".+ Say.Hello(.+)"));
    }

    @Test
    void testWrappedWildcards() {
        // ".*methodName.*" - method name surrounded by wildcards → exact name
        assertEquals("*method2*", RulesXmlMigrations.convertRegexToGlob(".*method2.*"));

        // ".+methodName.+" - same but with .+
        assertEquals("*Ping*", RulesXmlMigrations.convertRegexToGlob(".+Ping.+"));
    }

    @Test
    void testComplexRegexIgnored() {
        // Regex patterns that match signatures but cannot be cleanly converted to glob — ignored
        assertNull(RulesXmlMigrations.convertRegexToGlob("[a-z]+Method"));
        assertNull(RulesXmlMigrations.convertRegexToGlob("(foo|bar)"));
        assertNull(RulesXmlMigrations.convertRegexToGlob(".* (foo|bar).*"));
        assertNull(RulesXmlMigrations.convertRegexToGlob(".* foo\\..*"));
        assertNull(RulesXmlMigrations.convertRegexToGlob(".* foob?.*"));
        assertNull(RulesXmlMigrations.convertRegexToGlob(".* fo+.*"));
        assertNull(RulesXmlMigrations.convertRegexToGlob(".* foo[b].*"));
        assertNull(RulesXmlMigrations.convertRegexToGlob(".* fo{2}.*"));
        assertNull(RulesXmlMigrations.convertRegexToGlob(".* f\\o.*"));
        assertNull(RulesXmlMigrations.convertRegexToGlob(".* f o.*"));
    }

    @Test
    void testFullQualified() {
        // Regex patterns that match signatures but cannot be cleanly converted to glob — ignored
        assertEquals("getTest*", RulesXmlMigrations.convertRegexToGlob("void getTest(.*)"));
        assertEquals("getTest*", RulesXmlMigrations.convertRegexToGlob("java.lang.String getTest(.*)"));
        assertEquals("getTest", RulesXmlMigrations.convertRegexToGlob("java\\.lang\\.String getTest\\(.*\\)"));
        assertEquals("getTest", RulesXmlMigrations.convertRegexToGlob("java\\.lang\\.String getTest\\(int, long\\)"));
    }

    @Test
    void testSimpleNameIgnored() {
        // A plain method name like "myMethod" is not a valid method-filter regexp
        // because it cannot match any signature "returnType name(args)"
        assertNull(RulesXmlMigrations.convertRegexToGlob("myMethod"));
    }
}
