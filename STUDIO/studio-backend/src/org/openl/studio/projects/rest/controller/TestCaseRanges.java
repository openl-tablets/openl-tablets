package org.openl.studio.projects.rest.controller;

import org.openl.rules.testmethod.TestSuiteMethod;
import org.openl.studio.common.exception.BadRequestException;

/** The range of test cases a request names, checked before the request is taken on. */
final class TestCaseRanges {

    private TestCaseRanges() {
    }

    /**
     * Refuses a range that names a case the test table does not have.
     *
     * <p>The run itself resolves the range on another thread, where a refusal would reach nobody; asked here, it
     * answers the request instead.
     */
    static void requireKnownCases(TestSuiteMethod testSuiteMethod, String testRanges) {
        try {
            testSuiteMethod.getIndices(testRanges);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("tests.run.range.unknown-case.message", new Object[]{e.getMessage()});
        }
    }
}
