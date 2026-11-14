package org.openl.rules.maven;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import org.openl.rules.testmethod.TestSuiteMethod;

/**
 * @author Yury Molchan
 */
class ReportNamesTest {

    private static TestSuiteMethod test(String module, String name) {
        var test = mock(TestSuiteMethod.class);
        when(test.getModuleName()).thenReturn(module);
        when(test.getName()).thenReturn(name);
        return test;
    }

    @Test
    void namesATestByItsOwnName() {
        assertEquals("OpenL.Main.PremiumTest", new ReportNames().of(test("Main", "PremiumTest")));
    }

    @Test
    void namesATestWithoutANameAfterTheTableItTests() {
        var names = new ReportNames();

        assertEquals("OpenL.Tests.SayHelloTest", names.of(test("Tests", "SayHello$Test$0")));
        assertEquals("OpenL.Tests.SayHelloTest-2", names.of(test("Tests", "SayHello$Test$7")));
        assertEquals("OpenL.Tests.SayHelloTest-3", names.of(test("Tests", "SayHello$Run$8")));
        assertEquals("OpenL.Other.SayHelloTest", names.of(test("Other", "SayHello$Test$9")));
    }

    @Test
    void neverGivesTwoTestsOneName() {
        var names = new ReportNames();

        assertEquals("OpenL.Main.SayHelloTest", names.of(test("Main", "SayHelloTest")));
        assertEquals("OpenL.Main.SayHelloTest-2", names.of(test("Main", "SayHello$Test$0")));
    }
}
