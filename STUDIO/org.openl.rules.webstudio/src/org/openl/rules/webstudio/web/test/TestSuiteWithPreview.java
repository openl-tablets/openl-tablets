package org.openl.rules.webstudio.web.test;

import org.openl.rules.data.IDataBase;
import org.openl.rules.testmethod.TestDescription;
import org.openl.rules.testmethod.TestSuite;
import org.openl.rules.testmethod.TestSuiteMethod;

public class TestSuiteWithPreview extends TestSuite {
    private final TestDescriptionWithPreview[] testsWithPreview;
    private final IDataBase db;

    public TestSuiteWithPreview(IDataBase db, TestSuiteMethod testSuiteMethod) {
        super(testSuiteMethod);
        this.db = db;
        testsWithPreview = initTestsWithPreview();
    }

    public TestSuiteWithPreview(IDataBase db, TestSuiteMethod testSuiteMethod, int... testIndices) {
        super(testSuiteMethod, testIndices);
        this.db = db;
        testsWithPreview = initTestsWithPreview();
    }

    public TestSuiteWithPreview(IDataBase db, TestDescription... tests) {
        super(tests);
        this.db = db;
        testsWithPreview = initTestsWithPreview();
    }

    @Override
    public TestDescription[] getTests() {
        return testsWithPreview;
    }

    @Override
    public TestDescription getTest(int testNumber) {
        return testsWithPreview[testNumber];
    }

    private TestDescriptionWithPreview[] initTestsWithPreview() {
        TestDescription[] tests = super.getTests();

        TestDescriptionWithPreview[] testsWithPreview = new TestDescriptionWithPreview[tests.length];
        for (int i = 0; i < tests.length; i++) {
            TestDescription test = tests[i];
            testsWithPreview[i] = new TestDescriptionWithPreview(test, db);
        }
        return testsWithPreview;
    }
}
