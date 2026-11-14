package org.openl.rules.maven;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import org.openl.meta.StringValue;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.testmethod.TestSuiteMethod;
import org.openl.types.IOpenMethod;

/**
 * @author Yury Molchan
 */
class ReportNamesTest {

    /** A test table with the given header, of the given name as the engine knows it. */
    private static TestSuiteMethod test(String module, String header, String name, String tested) {
        var syntaxNode = mock(TableSyntaxNode.class);
        when(syntaxNode.getHeaderLineValue()).thenReturn(new StringValue(header));
        var testedMethod = mock(IOpenMethod.class);
        when(testedMethod.getName()).thenReturn(tested);
        var test = mock(TestSuiteMethod.class);
        when(test.getModuleName()).thenReturn(module);
        when(test.getName()).thenReturn(name);
        when(test.getSyntaxNode()).thenReturn(syntaxNode);
        when(test.getTestedMethod()).thenReturn(testedMethod);
        return test;
    }

    /** A test table of {@code SayHello} with a name of its own. */
    private static TestSuiteMethod named(String module, String name) {
        return test(module, "Test SayHello " + name, name, "SayHello");
    }

    /** A test table of the given table without a name of its own, which the engine names as given. */
    private static TestSuiteMethod unnamed(String module, String generated, String tested) {
        return test(module, "Test " + tested, generated, tested);
    }

    @Test
    void namesATestByItsOwnName() {
        assertEquals("OpenL.Main.PremiumTest", new ReportNames().of(named("Main", "PremiumTest")));
    }

    @Test
    void keepsTheNameTheAuthorWrote() {
        assertEquals("OpenL.Main.Premium$Regression", new ReportNames().of(named("Main", "Premium$Regression")));
        assertEquals("OpenL.Main.Premium$Test$0", new ReportNames().of(named("Main", "Premium$Test$0")));
        // even one that looks like the name the engine gives
        assertEquals("OpenL.Main.SayHello$Test$0", new ReportNames().of(named("Main", "SayHello$Test$0")));
    }

    @Test
    void namesATestWithoutANameAfterTheTableItTests() {
        var names = new ReportNames();

        assertEquals("OpenL.Tests.SayHelloTest", names.of(unnamed("Tests", "SayHello$Test$0", "SayHello")));
        assertEquals("OpenL.Tests.SayHelloTest-2", names.of(unnamed("Tests", "SayHello$Test$7", "SayHello")));
        assertEquals("OpenL.Tests.SayHelloTest-3",
                names.of(test("Tests", "Run SayHello", "SayHello$Run$8", "SayHello")));
        assertEquals("OpenL.Other.SayHelloTest", names.of(unnamed("Other", "SayHello$Test$9", "SayHello")));
        assertEquals("OpenL.Main.Say$HelloTest", names.of(unnamed("Main", "Say$Hello$Test$0", "Say$Hello")));
    }

    @Test
    void neverGivesTwoTestsOneName() {
        var names = new ReportNames();

        assertEquals("OpenL.Main.SayHelloTest", names.of(named("Main", "SayHelloTest")));
        assertEquals("OpenL.Main.SayHelloTest-2", names.of(unnamed("Main", "SayHello$Test$0", "SayHello")));
        // a name of its own gets a number too when an unnamed table reported before took it
        assertEquals("OpenL.Other.SayHelloTest", names.of(unnamed("Other", "SayHello$Test$0", "SayHello")));
        assertEquals("OpenL.Other.SayHelloTest-2", names.of(named("Other", "SayHelloTest")));
    }
}
