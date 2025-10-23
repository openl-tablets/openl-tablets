package org.openl.rules.testmethod;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openl.CompiledOpenClass;
import org.openl.rules.types.OpenMethodDispatcher;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.impl.MethodDelegator;

/**
 * @author snshor
 */
public final class ProjectHelper {

    private ProjectHelper() {
    }

    public static TestSuiteMethod[] allTesters(IOpenClass openClass) {
        List<TestSuiteMethod> res = new ArrayList<>();
        for (IOpenMethod tester : openClass.getMethods()) {
            if (isTester(tester)) {
                res.add((TestSuiteMethod) tester);
            }
            if (tester instanceof OpenMethodDispatcher && isTester(((OpenMethodDispatcher) tester).getTargetMethod())) {
                res.add((TestSuiteMethod) ((OpenMethodDispatcher) tester).getTargetMethod());
            }
        }

        TestSuiteMethod[] testSuiteMethods = new TestSuiteMethod[res.size()];
        return res.toArray(testSuiteMethods);
    }

    /**
     * Checks if the tester is instance of {@link TestSuiteMethod}, if it has any parameters for testing(see
     * {@link TestSuiteMethod#isRunmethodTestable()}) and if there is no errors in it.
     *
     * @param tester instance of method that is considered to be a test.
     * @return true if tester is valid {@link TestSuiteMethod}.
     */
    private static boolean isTester(IOpenMethod tester) {
        if (tester instanceof TestSuiteMethod testSuiteMethod) {
            try {
                return !testSuiteMethod.isRunMethod() && testSuiteMethod.isRunmethodTestable();
            } catch (Exception e) {
                Logger log = LoggerFactory.getLogger(ProjectHelper.class);
                log.error(e.getMessage(), e);
                return false;
            }

        }
        return false;
    }

    /**
     * Get tests for tested method that have filled rules rows data for testing its functionality. Run methods and tests
     * with empty test cases are not being processed. If you need to get all test methods, including run methods and
     * empty ones, use {@link #isTestForMethod(TestSuiteMethod, IOpenMethod)}.
     */
    public static TestSuiteMethod[] testers(IOpenMethod tested, CompiledOpenClass openClass) {
        return openClass.getOpenClassWithErrors().getMethods().stream()
                .filter(ProjectHelper::isTester)
                .map(TestSuiteMethod.class::cast)
                .filter(tester -> isTestForMethod(tester, tested))
                .toArray(TestSuiteMethod[]::new);
    }

    /**
     * If tester is an instance of {@link TestSuiteMethod} and tested method object in tester is equal to tested we
     * consider tester is test for tested method.
     */
    public static boolean isTestForMethod(TestSuiteMethod tester, IOpenMethod tested) {
        IOpenMethod toTest = tester.getTestedMethod();
        if (toTest.equals(tested)) {
            return true;
        }
        if (toTest instanceof OpenMethodDispatcher && ((OpenMethodDispatcher) toTest).getCandidates()
                .contains(tested)) {
            return true;
        }
        if (tested instanceof MethodDelegator) {
            return isTestForMethod(tester, tested.getMethod());
        }
        if (tested instanceof OpenMethodDispatcher) {
            return isTestForMethod(tester, ((OpenMethodDispatcher) tested).getTargetMethod());
        }
        return false;
    }

    public static String getTestInfo(IOpenMethod testMethod) {
        if (testMethod instanceof TestSuiteMethod testSuiteMethod) {
            return getTestInfo(testSuiteMethod, testSuiteMethod.getNumberOfTestsCases());
        }
        return null;

    }

    public static String getTestInfo(TestSuite testSuite) {
        return getTestInfo(testSuite.getTestSuiteMethod(), testSuite.getNumberOfTests());
    }

    private static String getTestInfo(TestSuiteMethod testMethod, int numberOfTests) {
        String info = null;

        if (testMethod instanceof TestSuiteMethod testSuite) {
            if (testSuite.isRunMethod()) {
                if (numberOfTests < 1) {
                    info = "No runs";
                } else if (numberOfTests == 1) {
                    info = numberOfTests + " run";
                } else {
                    info = numberOfTests + " runs";
                }
            } else {
                if (numberOfTests < 1) {
                    info = "No test cases";
                } else if (numberOfTests == 1) {
                    info = numberOfTests + " test case";
                } else {
                    info = numberOfTests + " test cases";
                }
            }
        }

        return info;
    }
}
