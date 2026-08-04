package org.openl.rules.project.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;

import org.junit.jupiter.api.Test;

class RulesXmlMigrationsConvertRegexToGlobTest {

    @Test
    void testMatchAll() {
        assertGlobs(".*", "*");
        assertGlobs(".+", "*");
    }

    @Test
    void testNullAndBlank() {
        assertNoGlob(null);
        assertNoGlob("");
        assertNoGlob("  ");
    }

    @Test
    void testInvalidRegex() {
        // "*" is not valid regex — should be ignored
        assertNoGlob("*");
        // Unbalanced brackets
        assertNoGlob("[abc");
    }

    @Test
    void testRegexThatCannotMatchSignature() {
        // Valid regex but cannot match any method signature like "Type name(Args)"
        assertNoGlob("^\\d+$");
        assertNoGlob("^$");
    }

    @Test
    void testSignatureWithEscapedParens() {
        // ".+ methodName\(.+\)" - method with any return type and at least one param
        assertGlobs(".+ SayHello\\(.+\\)", "SayHello");

        // ".+ methodName\(.*\)" - method with any return type and optional params
        assertGlobs(".+ main1\\(.*\\)", "main1");
        assertGlobs(".* main2\\(.*\\)", "main2");

        // ".+ methodName\(\)" - method with no params
        assertGlobs(".+ mySpr\\(\\)", "mySpr");
    }

    @Test
    void testSignatureWithUnescapedParens() {
        // ".* getFactor(.*)" - unescaped parens
        assertGlobs(".* getFactor(.*)", "getFactor*");
        assertGlobs(".+ SayHello(.+)", "SayHello*");
        assertGlobs(".+ Say.Hello(.+)", "Say?Hello*");
    }

    @Test
    void testWrappedWildcards() {
        // ".*methodName.*" - method name surrounded by wildcards → exact name
        assertGlobs(".*method2.*", "*method2*");

        // ".+methodName.+" - same but with .+
        assertGlobs(".+Ping.+", "*Ping*");
    }

    @Test
    void testAlternationUnfoldsIntoNames() {
        // A (a|b) group unfolds into one glob per branch — the whole point of EPBDS-16365.
        assertGlobs(".+ calc(Rate|Premium)\\(.+\\)", "calcRate", "calcPremium");
        assertGlobs(".+ (_api_deduct|premium)\\(.*\\)", "_api_deduct", "premium");
        // A single-branch group just drops its parentheses.
        assertGlobs(".+ (myRule7)\\(.+\\)", "myRule7");
        // Three branches.
        assertGlobs(".+ (a|b|c)\\(.*\\)", "a", "b", "c");
    }

    @Test
    void testTopLevelAlternationSplitsIntoSignatures() {
        // Two full signatures joined by | — each converts on its own, so neither branch is lost.
        assertGlobs(".+ calcRate\\(.+\\)|.+ calcPremium\\(.+\\)", "calcRate", "calcPremium");
    }

    @Test
    void testAlternationCombinedWithWildcards() {
        // The branches still reduce their own wildcards.
        assertGlobs(".* (foo|bar).*", "foo*", "bar*");
    }

    @Test
    void testAlternationFailsWholePatternWhenAnyBranchDoesNot() {
        // "foo" alone is not a signature, so the whole (foo|bar) yields nothing — the filter is kept.
        assertNoGlob("(foo|bar)");
    }

    @Test
    void testEmptyNameSignatureYieldsNoGlob() {
        // A signature whose name reduces to nothing (".+ \(\)": return type and params stripped leave "")
        // must not add an empty include glob; the filter is kept instead.
        assertNoGlob(".+ \\(\\)");
        assertNoGlob(".* \\(\\)");
    }

    @Test
    void testPathologicalAlternationIsKept() {
        // Nine nested groups would expand to 2^9 branches; past the cap the pattern is kept as a filter
        // rather than unfolded, so a crafted method-filter can never OOM the migration.
        assertNoGlob(".+ " + "(a|b)".repeat(9) + "\\(.+\\)");
    }

    @Test
    void testComplexRegexIgnored() {
        // Regex patterns that match signatures but cannot be cleanly converted to glob — ignored
        assertNoGlob("[a-z]+Method");
        assertNoGlob(".* foo\\..*");
        assertNoGlob(".* foob?.*");
        assertNoGlob(".* fo+.*");
        assertNoGlob(".* foo[b].*");
        assertNoGlob(".* fo{2}.*");
        assertNoGlob(".* f\\o.*");
        assertNoGlob(".* f o.*");
    }

    @Test
    void testFullQualified() {
        assertGlobs("void getTest(.*)", "getTest*");
        assertGlobs("java.lang.String getTest(.*)", "getTest*");
        assertGlobs("java\\.lang\\.String getTest\\(.*\\)", "getTest");
        assertGlobs("java\\.lang\\.String getTest\\(int, long\\)", "getTest");
    }

    @Test
    void testSimpleNameIgnored() {
        // A plain method name like "myMethod" is not a valid method-filter regexp
        // because it cannot match any signature "returnType name(args)"
        assertNoGlob("myMethod");
    }

    private static void assertGlobs(String regex, String... expected) {
        assertEquals(Set.of(expected), RulesXmlMigrations.convertRegexToGlobs(regex));
    }

    private static void assertNoGlob(String regex) {
        assertEquals(Set.of(), RulesXmlMigrations.convertRegexToGlobs(regex));
    }
}
